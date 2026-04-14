package com.jtexpress.app

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CreateOrderFragment : Fragment() {

    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    // ── UI References ─────────────────────────────────────────────
    private var btnBack: ImageButton? = null
    private var llSender: LinearLayout? = null
    private var llRecipient: LinearLayout? = null
    private var tvSenderSummary: TextView? = null
    private var tvRecipientSummary: TextView? = null

    // Pickup/Dropoff tab
    private var tabDropoff: TextView? = null
    private var tabPickup: TextView? = null
    private var llDropoffContent: LinearLayout? = null
    private var llPickupContent: LinearLayout? = null
    private var llSelectBranch: LinearLayout? = null
    private var tvBranchName: TextView? = null
    private var tvBranchAddress: TextView? = null
    private var tvBranchDistance: TextView? = null

    // Pickup schedule
    private var llPickupDate: LinearLayout? = null
    private var tvPickupDate: TextView? = null
    private var btnSlotMorning: TextView? = null
    private var btnSlotAfternoon: TextView? = null

    // Express type
    private var progressParcelTypes: ProgressBar? = null
    private var llParcelTypes: LinearLayout? = null

    // Item description
    private var etItemName: EditText? = null
    private var llCategorySpinner: LinearLayout? = null
    private var tvCategory: TextView? = null
    private var etQuantity: EditText? = null
    private var etWeight: EditText? = null
    private var etGoodsValue: EditText? = null
    private var llToggleDimensions: LinearLayout? = null
    private var ivDimensionsArrow: ImageView? = null
    private var llDimensions: LinearLayout? = null
    private var etLength: EditText? = null
    private var etWidth: EditText? = null
    private var etHeight: EditText? = null
    private var llVolWeightDisplay: LinearLayout? = null
    private var tvVolWeight: TextView? = null
    private var llChargeableDisplay: LinearLayout? = null
    private var tvChargeableWeight: TextView? = null

    // Voucher
    private var etVoucherCode: EditText? = null
    private var btnApplyVoucher: MaterialButton? = null
    private var tvVoucherStatus: TextView? = null

    // Cost
    private var tvCostShipment: TextView? = null
    private var tvCostService: TextView? = null
    private var tvCostValuation: TextView? = null
    private var llCostVoucherRow: LinearLayout? = null
    private var tvCostVoucherDisc: TextView? = null
    private var tvCostTotal: TextView? = null

    // Terms + Submit
    private var cbTerms: CheckBox? = null
    private var btnSubmit: MaterialButton? = null
    private var progressSubmit: ProgressBar? = null

    // ── State ──────────────────────────────────────────────────────
    private var pickupMode = "DropOff"   // "DropOff" or "DoorToDoor"
    private var selectedSlot = ""

    // Sender/Recipient data maps (populated via bottom sheets / dialogs)
    private var senderData: MutableMap<String, Any> = mutableMapOf()
    private var recipientData: MutableMap<String, Any> = mutableMapOf()

    // Selected branch from Firestore
    private var selectedBranchId: String? = null
    private var selectedBranchName: String = ""
    private var selectedBranchAddress: String = ""

    // Selected parcel type
    private var selectedParcelTypeId: String? = null
    private var selectedParcelTypeName: String = ""
    private var selectedServiceFee: Double = 0.0

    // Selected category
    private var selectedCategory: String = ""

    // Voucher
    private var appliedVoucherId: String? = null
    private var voucherDiscount: Double = 0.0

    // Cost fields
    private var shipmentFee: Double = 0.0
    private var serviceFee: Double = 0.0
    private var valuationFee: Double = 0.0

    // Categories list loaded from Firestore (or static fallback)
    private val itemCategories = mutableListOf<String>()

    // Parcel types loaded from Firestore
    data class ParcelType(
        val id: String,
        val code: String,
        val name: String,
        val deliveryDays: String,
        val serviceFee: Double
    )
    private val parcelTypes = mutableListOf<ParcelType>()

    // Branches loaded from Firestore
    data class Branch(
        val id: String,
        val name: String,
        val city: String,
        val address: String
    )
    private val branches = mutableListOf<Branch>()

    companion object {
        private const val TAG = "CreateOrderFragment"
        // Fallback categories if Firestore doesn't have them configured
        private val DEFAULT_CATEGORIES = listOf(
            "Clothing & Apparel",
            "Electronics",
            "Documents",
            "Food & Beverages",
            "Health & Beauty",
            "Toys & Games",
            "Books & Stationery",
            "Jewelry & Accessories",
            "Home & Living",
            "Sports & Outdoors",
            "Tools & Hardware",
            "Other"
        )
    }

    // ── Lifecycle ─────────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_order, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        initViews(view)
        setupTabListeners()
        setupDimensionsToggle()
        setupWeightWatchers()
        setupClickListeners()
        loadParcelTypes()
        loadBranches()
        loadCategories()
        prefillSenderFromProfile()
    }

    // ── Init views ────────────────────────────────────────────────
    private fun initViews(view: View) {
        btnBack              = view.findViewById(R.id.btn_back)
        llSender             = view.findViewById(R.id.ll_sender)
        llRecipient          = view.findViewById(R.id.ll_recipient)
        tvSenderSummary      = view.findViewById(R.id.tv_sender_summary)
        tvRecipientSummary   = view.findViewById(R.id.tv_recipient_summary)

        tabDropoff           = view.findViewById(R.id.tab_dropoff)
        tabPickup            = view.findViewById(R.id.tab_pickup)
        llDropoffContent     = view.findViewById(R.id.ll_dropoff_content)
        llPickupContent      = view.findViewById(R.id.ll_pickup_content)
        llSelectBranch       = view.findViewById(R.id.ll_select_branch)
        tvBranchName         = view.findViewById(R.id.tv_branch_name)
        tvBranchAddress      = view.findViewById(R.id.tv_branch_address)
        tvBranchDistance     = view.findViewById(R.id.tv_branch_distance)

        llPickupDate         = view.findViewById(R.id.ll_pickup_date)
        tvPickupDate         = view.findViewById(R.id.tv_pickup_date)
        btnSlotMorning       = view.findViewById(R.id.btn_slot_morning)
        btnSlotAfternoon     = view.findViewById(R.id.btn_slot_afternoon)

        progressParcelTypes  = view.findViewById(R.id.progress_parcel_types)
        llParcelTypes        = view.findViewById(R.id.ll_parcel_types)

        etItemName           = view.findViewById(R.id.et_item_name)
        llCategorySpinner    = view.findViewById(R.id.ll_category_spinner)
        tvCategory           = view.findViewById(R.id.tv_category)
        etQuantity           = view.findViewById(R.id.et_quantity)
        etWeight             = view.findViewById(R.id.et_weight)
        etGoodsValue         = view.findViewById(R.id.et_goods_value)
        llToggleDimensions   = view.findViewById(R.id.ll_toggle_dimensions)
        ivDimensionsArrow    = view.findViewById(R.id.iv_dimensions_arrow)
        llDimensions         = view.findViewById(R.id.ll_dimensions)
        etLength             = view.findViewById(R.id.et_length)
        etWidth              = view.findViewById(R.id.et_width)
        etHeight             = view.findViewById(R.id.et_height)
        llVolWeightDisplay   = view.findViewById(R.id.ll_vol_weight_display)
        tvVolWeight          = view.findViewById(R.id.tv_vol_weight)
        llChargeableDisplay  = view.findViewById(R.id.ll_chargeable_display)
        tvChargeableWeight   = view.findViewById(R.id.tv_chargeable_weight)

        etVoucherCode        = view.findViewById(R.id.et_voucher_code)
        btnApplyVoucher      = view.findViewById(R.id.btn_apply_voucher)
        tvVoucherStatus      = view.findViewById(R.id.tv_voucher_status)

        tvCostShipment       = view.findViewById(R.id.tv_cost_shipment)
        tvCostService        = view.findViewById(R.id.tv_cost_service)
        tvCostValuation      = view.findViewById(R.id.tv_cost_valuation)
        llCostVoucherRow     = view.findViewById(R.id.ll_cost_voucher_row)
        tvCostVoucherDisc    = view.findViewById(R.id.tv_cost_voucher_disc)
        tvCostTotal          = view.findViewById(R.id.tv_cost_total)

        cbTerms              = view.findViewById(R.id.cb_terms)
        btnSubmit            = view.findViewById(R.id.btn_submit_order)
        progressSubmit       = view.findViewById(R.id.progress_submit)
    }

    // ── 1. Tab: DropOff / Pickup ──────────────────────────────────
    private fun setupTabListeners() {
        tabDropoff?.setOnClickListener { selectTab("DropOff") }
        tabPickup?.setOnClickListener  { selectTab("DoorToDoor") }
    }

    private fun selectTab(mode: String) {
        pickupMode = mode
        val ctx = requireContext()

        if (mode == "DropOff") {
            tabDropoff?.setTextColor(ContextCompat.getColor(ctx, R.color.jt_red))
            tabDropoff?.setBackgroundResource(R.drawable.tab_selected_bg)
            tabPickup?.setTextColor(ContextCompat.getColor(ctx, R.color.jt_gray))
            tabPickup?.background = null
            llDropoffContent?.visibility = View.VISIBLE
            llPickupContent?.visibility  = View.GONE
        } else {
            tabPickup?.setTextColor(ContextCompat.getColor(ctx, R.color.jt_red))
            tabPickup?.setBackgroundResource(R.drawable.tab_selected_bg)
            tabDropoff?.setTextColor(ContextCompat.getColor(ctx, R.color.jt_gray))
            tabDropoff?.background = null
            llDropoffContent?.visibility = View.GONE
            llPickupContent?.visibility  = View.VISIBLE
        }
    }

    // ── 2. Load Parcel Types from Firestore ───────────────────────
    //    Collection: parcel_types
    //    Fields: code, name, deliveryDays, serviceFee, active (bool), order (number)
    private fun loadParcelTypes() {
        progressParcelTypes?.visibility = View.VISIBLE
        llParcelTypes?.visibility = View.GONE

        db!!.collection("parcel_types")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                parcelTypes.clear()
                val filtered = snapshot.documents
                    .filter { it.getBoolean("active") != false }
                    .sortedBy { it.getLong("order") ?: 0L }

                filtered.forEach { doc ->
                    parcelTypes.add(
                        ParcelType(
                            id          = doc.id,
                            code        = doc.getString("code").orEmpty(),
                            name        = doc.getString("name").orEmpty(),
                            deliveryDays= doc.getString("deliveryDays").orEmpty(),
                            serviceFee  = doc.getDouble("serviceFee") ?: 0.0
                        )
                    )
                }

                progressParcelTypes?.visibility = View.GONE
                llParcelTypes?.visibility = View.VISIBLE

                if (parcelTypes.isEmpty()) {
                    // Fallback: show static SP and EZ cards
                    buildStaticParcelTypeCards()
                } else {
                    buildParcelTypeCards(parcelTypes)
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Log.e(TAG, "parcel_types load failed: ${e.message}")
                progressParcelTypes?.visibility = View.GONE
                llParcelTypes?.visibility = View.VISIBLE
                buildStaticParcelTypeCards()
            }
    }

    private fun buildStaticParcelTypeCards() {
        val staticTypes = listOf(
            ParcelType("sp", "SP", "J&T Super", "3-5", 25.0),
            ParcelType("ez", "EZ", "EZ", "5-7", 0.0)
        )
        buildParcelTypeCards(staticTypes)
    }

    private fun buildParcelTypeCards(types: List<ParcelType>) {
        llParcelTypes?.removeAllViews()
        val ctx = requireContext()
        val dm  = resources.displayMetrics

        types.forEachIndexed { index, pt ->
            val cardWidth = LinearLayout.LayoutParams(
                (150 * dm.density).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, (10 * dm.density).toInt(), 0)
            }

            val card = CardView(ctx).apply {
                layoutParams    = cardWidth
                radius          = 10f * dm.density
                cardElevation   = 2f * dm.density
                setCardBackgroundColor(ContextCompat.getColor(ctx,
                    if (index == 0) R.color.jt_red else android.R.color.white))
                isClickable     = true
                isFocusable     = true
            }

            val inner = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                val p = (12 * dm.density).toInt()
                setPadding(p, p, p, p)
            }

            val tvCode = TextView(ctx).apply {
                text      = pt.code
                textSize  = 11f
                setTextColor(ContextCompat.getColor(ctx,
                    if (index == 0) android.R.color.white else R.color.jt_gray))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val tvName = TextView(ctx).apply {
                text      = pt.name
                textSize  = 14f
                setTextColor(ContextCompat.getColor(ctx,
                    if (index == 0) android.R.color.white else R.color.jt_black))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, (4 * dm.density).toInt(), 0, 0)
            }

            val tvFee = TextView(ctx).apply {
                text     = if (pt.serviceFee > 0) "Service Fee: ${pt.serviceFee.toInt()} PHP" else "No service fee"
                textSize = 11f
                setTextColor(ContextCompat.getColor(ctx,
                    if (index == 0) android.R.color.white else R.color.jt_gray))
                setPadding(0, (2 * dm.density).toInt(), 0, 0)
            }

            val tvDays = TextView(ctx).apply {
                text     = if (pt.deliveryDays.isNotEmpty()) "${pt.deliveryDays} days" else ""
                textSize = 10f
                setTextColor(ContextCompat.getColor(ctx,
                    if (index == 0) android.R.color.white else R.color.jt_gray))
                setPadding(0, (2 * dm.density).toInt(), 0, 0)
            }

            inner.addView(tvCode)
            inner.addView(tvName)
            inner.addView(tvFee)
            if (tvDays.text.isNotEmpty()) inner.addView(tvDays)
            card.addView(inner)

            card.setOnClickListener {
                selectParcelType(pt, types)
            }

            llParcelTypes?.addView(card)

            // Auto-select the first type
            if (index == 0) selectParcelType(pt, types)
        }
    }

    private fun selectParcelType(selected: ParcelType, allTypes: List<ParcelType>) {
        selectedParcelTypeId   = selected.id
        selectedParcelTypeName = selected.name
        selectedServiceFee     = selected.serviceFee

        val ctx = requireContext()
        // Update card visuals
        val container = llParcelTypes ?: return
        allTypes.forEachIndexed { i, pt ->
            val card = container.getChildAt(i) as? CardView ?: return@forEachIndexed
            val isSelected = pt.id == selected.id
            card.setCardBackgroundColor(ContextCompat.getColor(ctx,
                if (isSelected) R.color.jt_red else android.R.color.white))
            // Update text colors inside
            val inner = card.getChildAt(0) as? LinearLayout ?: return@forEachIndexed
            for (j in 0 until inner.childCount) {
                (inner.getChildAt(j) as? TextView)?.setTextColor(
                    ContextCompat.getColor(ctx,
                        if (isSelected) android.R.color.white else R.color.jt_black))
            }
        }
        recalculateCost()
    }

    // ── 3. Load Branches from Firestore ──────────────────────────
    //    Collection: branches
    //    Fields: branchName, province, city, branchAddress, contactNumber, operatingHours
    private fun loadBranches() {
        db!!.collection("branches")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                branches.clear()
                snapshot.documents.forEach { doc ->
                    branches.add(
                        Branch(
                            id      = doc.id,
                            name    = doc.getString("branchName").orEmpty(),
                            city    = doc.getString("city").orEmpty(),
                            address = doc.getString("branchAddress").orEmpty()
                        )
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "branches load failed: ${e.message}")
            }
    }

    // ── 4. Load Categories ────────────────────────────────────────
    //    Optional: app_config/categories document with a "list" array field
    private fun loadCategories() {
        db!!.collection("app_config").document("categories")
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                @Suppress("UNCHECKED_CAST")
                val list = doc?.get("list") as? List<String>
                if (!list.isNullOrEmpty()) {
                    itemCategories.clear()
                    itemCategories.addAll(list)
                } else {
                    itemCategories.addAll(DEFAULT_CATEGORIES)
                }
            }
            .addOnFailureListener {
                itemCategories.addAll(DEFAULT_CATEGORIES)
            }
    }

    // ── 5. Prefill sender from saved profile ──────────────────────
    private fun prefillSenderFromProfile() {
        val prefs = requireContext().getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
        val fullName = prefs.getString("user_name", "") ?: ""
        val phone    = prefs.getString("user_phone", "") ?: ""

        if (fullName.isNotEmpty()) {
            val first = fullName.substringBefore(" ").trim()
            val last  = fullName.substringAfter(" ", "").trim()
            senderData["firstName"]   = first
            senderData["lastName"]    = last
            senderData["mobileNumber"]= phone
            updateSenderSummary()
        }
    }

    private fun updateSenderSummary() {
        val name  = "${senderData["firstName"]} ${senderData["lastName"]}".trim()
        val city  = senderData["city"] as? String ?: ""
        val phone = senderData["mobileNumber"] as? String ?: ""

        val parts = listOf(name, city, phone).filter { it.isNotEmpty() }
        tvSenderSummary?.text = if (parts.isNotEmpty()) parts.joinToString(" • ")
        else "Tap here to fill up sender information"
        tvSenderSummary?.setTextColor(ContextCompat.getColor(requireContext(),
            if (parts.isNotEmpty()) R.color.jt_black else R.color.jt_gray))
    }

    private fun updateRecipientSummary() {
        val name  = "${recipientData["firstName"]} ${recipientData["lastName"]}".trim()
        val city  = recipientData["city"] as? String ?: ""
        val phone = recipientData["mobileNumber"] as? String ?: ""

        val parts = listOf(name, city, phone).filter { it.isNotEmpty() }
        tvRecipientSummary?.text = if (parts.isNotEmpty()) parts.joinToString(" • ")
        else "Tap here to fill up receiver information"
        tvRecipientSummary?.setTextColor(ContextCompat.getColor(requireContext(),
            if (parts.isNotEmpty()) R.color.jt_black else R.color.jt_gray))
    }

    // ── 6. Dimensions toggle + auto-compute ───────────────────────
    private fun setupDimensionsToggle() {
        var expanded = false
        llToggleDimensions?.setOnClickListener {
            expanded = !expanded
            llDimensions?.visibility = if (expanded) View.VISIBLE else View.GONE
            ivDimensionsArrow?.rotation = if (expanded) 180f else 0f
            if (!expanded) {
                llVolWeightDisplay?.visibility  = View.GONE
                llChargeableDisplay?.visibility = View.GONE
            }
        }
    }

    private fun setupWeightWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { computeVolumeWeight() }
        }
        etLength?.addTextChangedListener(watcher)
        etWidth?.addTextChangedListener(watcher)
        etHeight?.addTextChangedListener(watcher)
        etWeight?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                computeVolumeWeight()
                recalculateCost()
            }
        })
        etGoodsValue?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { recalculateCost() }
        })
    }

    private fun computeVolumeWeight() {
        val l = etLength?.text?.toString()?.toDoubleOrNull() ?: 0.0
        val w = etWidth?.text?.toString()?.toDoubleOrNull()  ?: 0.0
        val h = etHeight?.text?.toString()?.toDoubleOrNull() ?: 0.0

        if (l > 0 && w > 0 && h > 0) {
            val volWeight       = (l * w * h) / 6000.0
            val actualWeight    = etWeight?.text?.toString()?.toDoubleOrNull() ?: 0.0
            val chargeableWeight= maxOf(volWeight, actualWeight)

            llVolWeightDisplay?.visibility  = View.VISIBLE
            llChargeableDisplay?.visibility = View.VISIBLE
            tvVolWeight?.text          = "%.2f KG".format(volWeight)
            tvChargeableWeight?.text   = "%.2f KG".format(chargeableWeight)
        } else {
            llVolWeightDisplay?.visibility  = View.GONE
            llChargeableDisplay?.visibility = View.GONE
        }
    }

    // ── 7. Cost calculation ───────────────────────────────────────
    //    ShipmentFee = chargeableWeight * 50 (base rate, adjust as needed)
    //    ValuationFee = goodsValue * 0.005
    //    ServiceFee = from selected parcel type
    //    Total = ShipmentFee + ValuationFee + ServiceFee - voucherDiscount
    private fun recalculateCost() {
        val l = etLength?.text?.toString()?.toDoubleOrNull() ?: 0.0
        val w = etWidth?.text?.toString()?.toDoubleOrNull()  ?: 0.0
        val h = etHeight?.text?.toString()?.toDoubleOrNull() ?: 0.0
        val actualWeight    = etWeight?.text?.toString()?.toDoubleOrNull() ?: 0.0
        val volWeight       = if (l > 0 && w > 0 && h > 0) (l * w * h) / 6000.0 else 0.0
        val chargeableWeight= maxOf(volWeight, actualWeight)
        val goodsValue      = etGoodsValue?.text?.toString()?.toDoubleOrNull() ?: 0.0

        shipmentFee  = chargeableWeight * 50.0  // PHP 50 per KG base rate
        valuationFee = goodsValue * 0.005        // 0.5% valuation
        serviceFee   = selectedServiceFee

        val total = shipmentFee + valuationFee + serviceFee - voucherDiscount

        tvCostShipment?.text = "PHP %.2f".format(shipmentFee)
        tvCostService?.text  = "PHP %.2f".format(serviceFee)
        tvCostValuation?.text= "PHP %.2f".format(valuationFee)
        tvCostTotal?.text    = "PHP %.0f".format(maxOf(total, 0.0))

        if (voucherDiscount > 0) {
            llCostVoucherRow?.visibility = View.VISIBLE
            tvCostVoucherDisc?.text = "- PHP %.2f".format(voucherDiscount)
        } else {
            llCostVoucherRow?.visibility = View.GONE
        }
    }

    // ── 8. Click listeners ────────────────────────────────────────
    private fun setupClickListeners() {

        btnBack?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Sender dialog
        llSender?.setOnClickListener { showAddressDialog(isSender = true) }

        // Recipient dialog
        llRecipient?.setOnClickListener { showAddressDialog(isSender = false) }

        // Branch picker
        llSelectBranch?.setOnClickListener { showBranchPickerDialog() }

        // Pickup date picker
        llPickupDate?.setOnClickListener { showDatePicker() }

        // Time slots
        btnSlotMorning?.setOnClickListener   { selectTimeSlot("9AM–12PM") }
        btnSlotAfternoon?.setOnClickListener { selectTimeSlot("1PM–5PM") }

        // Category picker
        llCategorySpinner?.setOnClickListener { showCategoryDialog() }

        // Voucher apply
        btnApplyVoucher?.setOnClickListener { applyVoucher() }

        // Submit
        btnSubmit?.setOnClickListener { validateAndSubmit() }
    }

    // ── Address Dialog (Sender / Recipient) ───────────────────────
    private fun showAddressDialog(isSender: Boolean) {
        val title = if (isSender) "Sender Information" else "Recipient Information"
        val data  = if (isSender) senderData else recipientData

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_address_form, null)

        val etFirstName = dialogView.findViewById<EditText>(R.id.et_dialog_first_name)
        val etLastName  = dialogView.findViewById<EditText>(R.id.et_dialog_last_name)
        val etPhone     = dialogView.findViewById<EditText>(R.id.et_dialog_phone)
        val etProvince  = dialogView.findViewById<EditText>(R.id.et_dialog_province)
        val etCity      = dialogView.findViewById<EditText>(R.id.et_dialog_city)
        val etBarangay  = dialogView.findViewById<EditText>(R.id.et_dialog_barangay)
        val etStreet    = dialogView.findViewById<EditText>(R.id.et_dialog_street)

        // Prefill if already filled
        etFirstName.setText(data["firstName"] as? String ?: "")
        etLastName.setText(data["lastName"]  as? String ?: "")
        etPhone.setText(data["mobileNumber"] as? String ?: "")
        etProvince.setText(data["province"]  as? String ?: "")
        etCity.setText(data["city"]          as? String ?: "")
        etBarangay.setText(data["barangay"]  as? String ?: "")
        etStreet.setText(data["street"]      as? String ?: "")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                val targetMap = if (isSender) senderData else recipientData
                targetMap["firstName"]   = etFirstName.text.toString().trim()
                targetMap["lastName"]    = etLastName.text.toString().trim()
                targetMap["mobileNumber"]= etPhone.text.toString().trim()
                targetMap["province"]    = etProvince.text.toString().trim()
                targetMap["city"]        = etCity.text.toString().trim()
                targetMap["barangay"]    = etBarangay.text.toString().trim()
                targetMap["street"]      = etStreet.text.toString().trim()

                if (isSender) updateSenderSummary()
                else          updateRecipientSummary()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Branch Picker Dialog ──────────────────────────────────────
    private fun showBranchPickerDialog() {
        if (branches.isEmpty()) {
            Toast.makeText(requireContext(), "Loading branches, please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        val names = branches.map { "${it.name} – ${it.city}" }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Drop-Off Branch")
            .setItems(names) { _, which ->
                val branch = branches[which]
                selectedBranchId      = branch.id
                selectedBranchName    = branch.name
                selectedBranchAddress = branch.address

                tvBranchName?.text    = branch.name
                tvBranchAddress?.text = branch.address.ifEmpty { branch.city }
                tvBranchDistance?.text= ""
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Date Picker ───────────────────────────────────────────────
    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 1) // Next day min

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                val picked = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                tvPickupDate?.text = sdf.format(picked.time)
                tvPickupDate?.setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_black))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = cal.timeInMillis
        }.show()
    }

    private fun selectTimeSlot(slot: String) {
        selectedSlot = slot
        val ctx = requireContext()
        btnSlotMorning?.setBackgroundResource(
            if (slot == "9AM–12PM") R.drawable.tab_selected_bg else R.drawable.rounded_border_bg)
        btnSlotMorning?.setTextColor(ContextCompat.getColor(ctx,
            if (slot == "9AM–12PM") R.color.jt_red else R.color.jt_gray))
        btnSlotAfternoon?.setBackgroundResource(
            if (slot == "1PM–5PM") R.drawable.tab_selected_bg else R.drawable.rounded_border_bg)
        btnSlotAfternoon?.setTextColor(ContextCompat.getColor(ctx,
            if (slot == "1PM–5PM") R.color.jt_red else R.color.jt_gray))
    }

    // ── Category Dialog ───────────────────────────────────────────
    private fun showCategoryDialog() {
        val cats = if (itemCategories.isEmpty()) DEFAULT_CATEGORIES else itemCategories
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Category")
            .setItems(cats.toTypedArray()) { _, which ->
                selectedCategory   = cats[which]
                tvCategory?.text   = selectedCategory
                tvCategory?.setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_black))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Voucher ───────────────────────────────────────────────────
    //    Collection: vouchers
    //    Fields: voucherCode (string), discountAmount (number), validFrom (date),
    //            validUntil (date), isUsed (bool)
    private fun applyVoucher() {
        val code = etVoucherCode?.text?.toString()?.trim().orEmpty()
        if (code.isEmpty()) {
            showVoucherStatus("Enter a voucher code first.", isError = true)
            return
        }

        btnApplyVoucher?.isEnabled = false
        btnApplyVoucher?.text = "..."

        db!!.collection("vouchers")
            .whereEqualTo("voucherCode", code)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                btnApplyVoucher?.isEnabled = true
                btnApplyVoucher?.text = "Apply"

                val doc = snapshot.documents.firstOrNull()
                if (doc == null) {
                    showVoucherStatus("Voucher code not found.", isError = true)
                    return@addOnSuccessListener
                }

                val isUsed = doc.getBoolean("isUsed") ?: false
                if (isUsed) {
                    showVoucherStatus("This voucher has already been used.", isError = true)
                    return@addOnSuccessListener
                }

                // Check validity dates
                val now = Date()
                val validFrom  = doc.getDate("validFrom")
                val validUntil = doc.getDate("validUntil")

                if (validFrom != null && now.before(validFrom)) {
                    showVoucherStatus("Voucher is not yet active.", isError = true)
                    return@addOnSuccessListener
                }
                if (validUntil != null && now.after(validUntil)) {
                    showVoucherStatus("Voucher has expired.", isError = true)
                    return@addOnSuccessListener
                }

                appliedVoucherId = doc.id
                voucherDiscount  = doc.getDouble("discountAmount") ?: 0.0
                showVoucherStatus("Voucher applied! -PHP %.2f".format(voucherDiscount), isError = false)
                recalculateCost()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                btnApplyVoucher?.isEnabled = true
                btnApplyVoucher?.text = "Apply"
                showVoucherStatus("Failed to validate voucher.", isError = true)
                Log.e(TAG, "Voucher error: ${e.message}")
            }
    }

    private fun showVoucherStatus(msg: String, isError: Boolean) {
        tvVoucherStatus?.visibility = View.VISIBLE
        tvVoucherStatus?.text       = msg
        tvVoucherStatus?.setTextColor(ContextCompat.getColor(requireContext(),
            if (isError) R.color.jt_red else android.R.color.holo_green_dark))
    }

    // ── Validate + Submit ─────────────────────────────────────────
    private fun validateAndSubmit() {
        // Validate sender
        if ((senderData["firstName"] as? String).isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please fill in sender information.", Toast.LENGTH_SHORT).show()
            return
        }
        // Validate recipient
        if ((recipientData["firstName"] as? String).isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please fill in recipient information.", Toast.LENGTH_SHORT).show()
            return
        }
        // Validate branch (drop-off mode)
        if (pickupMode == "DropOff" && selectedBranchId == null) {
            Toast.makeText(requireContext(), "Please select a drop-off branch.", Toast.LENGTH_SHORT).show()
            return
        }
        // Validate pickup schedule
        if (pickupMode == "DoorToDoor") {
            val date = tvPickupDate?.text?.toString() ?: ""
            if (date == "Select date" || date.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a pickup date.", Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedSlot.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a pickup time slot.", Toast.LENGTH_SHORT).show()
                return
            }
        }
        // Validate parcel type
        if (selectedParcelTypeId == null) {
            Toast.makeText(requireContext(), "Please select an express type.", Toast.LENGTH_SHORT).show()
            return
        }
        // Validate item name
        val itemName = etItemName?.text?.toString()?.trim() ?: ""
        if (itemName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an item name.", Toast.LENGTH_SHORT).show()
            return
        }
        // Validate weight
        val weight = etWeight?.text?.toString()?.toDoubleOrNull()
        if (weight == null || weight <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid weight.", Toast.LENGTH_SHORT).show()
            return
        }
        // Validate terms
        if (cbTerms?.isChecked != true) {
            Toast.makeText(requireContext(), "Please agree to the Terms and Conditions.", Toast.LENGTH_SHORT).show()
            return
        }

        submitOrder(itemName, weight)
    }

    private fun submitOrder(itemName: String, weight: Double) {
        val uid = auth?.currentUser?.uid ?: return
        btnSubmit?.isEnabled = false
        progressSubmit?.visibility = View.VISIBLE

        // Generate waybill number: JT + timestamp + 4 random digits
        val waybillNumber = "JT${System.currentTimeMillis()}${(1000..9999).random()}"
        val bookingDate   = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())

        val l           = etLength?.text?.toString()?.toDoubleOrNull() ?: 0.0
        val w           = etWidth?.text?.toString()?.toDoubleOrNull()  ?: 0.0
        val h           = etHeight?.text?.toString()?.toDoubleOrNull() ?: 0.0
        val volWeight   = if (l > 0 && w > 0 && h > 0) (l * w * h) / 6000.0 else 0.0
        val chargeable  = maxOf(volWeight, weight)
        val goodsValue  = etGoodsValue?.text?.toString()?.toDoubleOrNull() ?: 0.0
        val quantity    = etQuantity?.text?.toString()?.toIntOrNull() ?: 1
        val totalAmount = maxOf(shipmentFee + valuationFee + serviceFee - voucherDiscount, 0.0)

        // Build origin/destination display strings
        val originCity      = senderData["city"] as? String ?: ""
        val destinationCity = recipientData["city"] as? String ?: ""

        // ── Booking document ──
        val bookingData = hashMapOf(
            "userId"          to uid,
            "shipmentType"    to "Domestic",
            "pickupMode"      to pickupMode,
            "bookingStatus"   to "Processing",
            "bookingDate"     to bookingDate,
            "waybillNumber"   to waybillNumber,
            "barcodeValue"    to waybillNumber,
            "qrCodeValue"     to waybillNumber,
            "createdAt"       to com.google.firebase.Timestamp.now(),

            // Sender
            "senderFirstName"   to (senderData["firstName"] ?: ""),
            "senderLastName"    to (senderData["lastName"]  ?: ""),
            "senderPhone"       to (senderData["mobileNumber"] ?: ""),
            "senderProvince"    to (senderData["province"]  ?: ""),
            "senderCity"        to (senderData["city"]      ?: ""),
            "senderBarangay"    to (senderData["barangay"]  ?: ""),
            "senderStreet"      to (senderData["street"]    ?: ""),

            // Recipient
            "recipientFirstName"  to (recipientData["firstName"] ?: ""),
            "recipientLastName"   to (recipientData["lastName"]  ?: ""),
            "recipientPhone"      to (recipientData["mobileNumber"] ?: ""),
            "recipientProvince"   to (recipientData["province"]  ?: ""),
            "recipientCity"       to (recipientData["city"]      ?: ""),
            "recipientBarangay"   to (recipientData["barangay"]  ?: ""),
            "recipientStreet"     to (recipientData["street"]    ?: ""),

            // Package
            "itemName"          to itemName,
            "itemCategory"      to selectedCategory,
            "quantity"          to quantity,
            "weight"            to weight,
            "length"            to l,
            "width"             to w,
            "height"            to h,
            "volumeWeight"      to volWeight,
            "chargeableWeight"  to chargeable,
            "goodsValue"        to goodsValue,

            // Parcel type
            "parcelTypeId"      to (selectedParcelTypeId ?: ""),
            "parcelTypeName"    to selectedParcelTypeName,

            // Branch (drop-off)
            "branchId"          to (selectedBranchId ?: ""),
            "branchName"        to selectedBranchName,

            // Pickup schedule (door-to-door)
            "pickupDate"        to (tvPickupDate?.text?.toString() ?: ""),
            "pickupTimeSlot"    to selectedSlot,

            // Cost
            "shipmentFee"       to shipmentFee,
            "serviceFee"        to serviceFee,
            "valuationFee"      to valuationFee,
            "voucherDiscount"   to voucherDiscount,
            "totalAmount"       to totalAmount,
            "voucherId"         to (appliedVoucherId ?: ""),

            // For HomeFragment compatibility
            "trackingNumber"    to waybillNumber,
            "status"            to "Pending",
            "origin"            to originCity,
            "destination"       to destinationCity
        )

        db!!.collection("orders")
            .add(bookingData)
            .addOnSuccessListener { docRef ->
                if (!isAdded) return@addOnSuccessListener

                // Mark voucher as used
                if (appliedVoucherId != null) {
                    db!!.collection("vouchers").document(appliedVoucherId!!)
                        .update("isUsed", true)
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Voucher update failed: ${e.message}")
                        }
                }

                progressSubmit?.visibility = View.GONE
                btnSubmit?.isEnabled = true

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Order Submitted!")
                    .setMessage("Your booking has been placed.\n\nWaybill No: $waybillNumber\n\nYou can track your parcel using this number.")
                    .setPositiveButton("Go to My Orders") { _, _ ->
                        (activity as? MainActivity)?.switchToTab(R.id.nav_my_order)
                    }
                    .setNegativeButton("New Order") { _, _ ->
                        resetForm()
                    }
                    .setCancelable(false)
                    .show()

                Log.d(TAG, "Order created: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                progressSubmit?.visibility = View.GONE
                btnSubmit?.isEnabled = true
                Toast.makeText(requireContext(), "Failed to submit order. Please try again.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Order submit failed: ${e.message}")
            }
    }

    // ── Reset form after submission ────────────────────────────────
    private fun resetForm() {
        senderData.clear()
        recipientData.clear()
        selectedBranchId      = null
        selectedBranchName    = ""
        selectedBranchAddress = ""
        selectedParcelTypeId  = null
        selectedParcelTypeName= ""
        selectedServiceFee    = 0.0
        selectedCategory      = ""
        appliedVoucherId      = null
        voucherDiscount       = 0.0
        selectedSlot          = ""

        tvSenderSummary?.text    = "Tap here to fill up sender information"
        tvRecipientSummary?.text = "Tap here to fill up receiver information"
        tvBranchName?.text       = "Select a drop-off branch"
        tvBranchAddress?.text    = ""
        tvPickupDate?.text       = "Select date"
        tvCategory?.text         = "Select category"
        etItemName?.setText("")
        etQuantity?.setText("")
        etWeight?.setText("")
        etGoodsValue?.setText("")
        etLength?.setText("")
        etWidth?.setText("")
        etHeight?.setText("")
        etVoucherCode?.setText("")
        tvVoucherStatus?.visibility = View.GONE
        cbTerms?.isChecked = false
        selectTab("DropOff")

        prefillSenderFromProfile()
        recalculateCost()

        // Re-select first parcel type
        if (parcelTypes.isNotEmpty()) selectParcelType(parcelTypes[0], parcelTypes)
    }
}