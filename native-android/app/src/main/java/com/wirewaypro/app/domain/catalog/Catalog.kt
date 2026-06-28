package com.wirewaypro.app.domain.catalog

/**
 * NEC-2023 residential service catalog, ported verbatim from the web app's
 * src/catalog.js (the data behind the quote `entries`). Each service has separate
 * material + labor costs and labor hours; variants multiply both via [CatalogVariant.m].
 *
 * Numbers must match the web file exactly — they feed [QuoteCalculator] so native
 * estimates equal the React app to the dollar.
 */
data class CatalogVariant(val label: String, val m: Double)

data class CatalogService(
    val id: String,
    val label: String,
    val nec: String,
    val materialCost: Double,
    val laborCost: Double,
    val laborHours: Double,
    val unit: String,
    val variants: List<CatalogVariant>,
)

data class CatalogCategory(
    val id: String,
    val label: String,
    val services: List<CatalogService>,
)

private fun s(
    id: String, label: String, nec: String,
    materialCost: Double, laborCost: Double, laborHours: Double, unit: String,
    vararg variants: Pair<String, Double>,
) = CatalogService(id, label, nec, materialCost, laborCost, laborHours, unit, variants.map { CatalogVariant(it.first, it.second) })

object Catalog {

    val categories: List<CatalogCategory> = listOf(
        CatalogCategory("service_entrance", "Service Entrance & Panel", listOf(
            s("panel_100", "Panel Upgrade – 100A", "NEC 230.79(C)", 650.0, 595.0, 7.0, "panel", "Standard" to 1.0, "Main Breaker" to 1.15),
            s("panel_200", "Panel Upgrade – 200A", "NEC 230.79(D)", 1200.0, 850.0, 10.0, "panel", "Standard" to 1.0, "Main Breaker" to 1.15, "Smart Panel" to 1.6),
            s("panel_400", "Panel Upgrade – 400A", "NEC 230.79", 2800.0, 1360.0, 16.0, "panel", "Standard" to 1.0, "With Meter Stack" to 1.3),
            s("sub_panel", "Sub-Panel Install", "NEC 225.30", 500.0, 510.0, 6.0, "panel", "60A" to 1.0, "100A" to 1.2, "150A" to 1.4),
            s("breaker_single", "Breaker – Single Pole", "NEC 240.6", 60.0, 64.0, 0.75, "breaker", "15A Standard" to 1.0, "20A Standard" to 1.05, "AFCI" to 1.65, "GFCI" to 1.65, "Dual AFCI/GFCI" to 1.85),
            s("breaker_double", "Breaker – Double Pole", "NEC 240.6", 90.0, 85.0, 1.0, "breaker", "30A" to 1.0, "40A" to 1.1, "50A" to 1.2, "60A" to 1.3),
            s("surge_whole", "Whole-Home Surge Protector", "NEC 230.67", 180.0, 128.0, 1.5, "unit", "Type 1" to 1.0, "Type 2" to 0.9, "Type 1+2 Combo" to 1.2),
            s("meter_socket", "Meter Socket Replacement", "NEC 230.66", 200.0, 213.0, 2.5, "unit", "100A" to 1.0, "200A" to 1.2),
            s("grounding", "Grounding Electrode System", "NEC 250.50", 120.0, 213.0, 2.5, "system", "Ground Rod" to 1.0, "Rod + Plate" to 1.4, "Ufer Ground" to 1.6),
            s("bonding", "Bonding – Water / Gas Pipe", "NEC 250.104", 40.0, 85.0, 1.0, "each", "Water Pipe" to 1.0, "Gas Pipe" to 1.0, "Both" to 1.8),
        )),
        CatalogCategory("circuits", "Branch Circuits & Wiring", listOf(
            s("circuit_15", "New 15A Circuit", "NEC 210.11", 120.0, 298.0, 3.5, "circuit", "Standard" to 1.0, "AFCI Required" to 1.2, "Long Run (50+ ft)" to 1.5),
            s("circuit_20", "New 20A Circuit", "NEC 210.11", 140.0, 340.0, 4.0, "circuit", "Standard" to 1.0, "Kitchen/Bath" to 1.15, "AFCI Required" to 1.25),
            s("circuit_30", "New 240V Circuit – 30A", "NEC 210.19", 180.0, 425.0, 5.0, "circuit", "Dryer" to 1.0, "HVAC" to 1.1, "Hot Tub/Spa" to 1.3),
            s("circuit_50", "New 240V Circuit – 50A", "NEC 210.19", 220.0, 510.0, 6.0, "circuit", "Range/Oven" to 1.0, "EV Charger" to 1.1, "Pool Equipment" to 1.2),
            s("circuit_dedicated", "Dedicated Appliance Circuit", "NEC 210.52", 130.0, 340.0, 4.0, "circuit", "Refrigerator" to 1.0, "Microwave" to 1.0, "Dishwasher" to 1.0, "Disposal" to 1.0),
            s("rewire_room", "Room Rewire", "NEC 310.15", 350.0, 680.0, 8.0, "room", "Bedroom" to 1.0, "Living Room" to 1.2, "Kitchen" to 1.8, "Bathroom" to 1.3),
            s("knob_tube", "Knob & Tube Removal/Replace", "NEC 394.12", 600.0, 1190.0, 14.0, "room", "Single Room" to 1.0, "Per Floor" to 3.0, "Whole House" to 7.0),
            s("aluminum_wiring", "Aluminum Wiring Remediation", "NEC 310.14(B)", 50.0, 34.0, 0.4, "outlet", "CO/ALR Device" to 1.0, "Pigtail w/ Marrette" to 1.3),
            s("low_voltage", "Low-Voltage / Data Run", "NEC 725.41", 60.0, 128.0, 1.5, "run", "Cat6 Ethernet" to 1.0, "Coax" to 0.9, "Speaker Wire" to 0.8, "HDMI/AV" to 1.1),
        )),
        CatalogCategory("receptacles", "Receptacles & Outlets", listOf(
            s("outlet_standard", "Standard Duplex Outlet", "NEC 210.52", 18.0, 21.0, 0.25, "outlet", "1-for-1 Swap" to 1.0, "New – Same Wall (15A)" to 2.4, "New – Same Wall (20A)" to 2.65),
            s("outlet_gfci", "GFCI Outlet", "NEC 210.8", 38.0, 25.0, 0.3, "outlet", "1-for-1 Swap" to 1.0, "New Install – 15A" to 2.3, "New Install – 20A" to 2.5, "WR + New Install" to 2.8),
            s("outlet_afci", "AFCI Outlet", "NEC 210.12", 55.0, 25.0, 0.3, "outlet", "1-for-1 Swap" to 1.0, "New Install – 15A" to 2.2, "New Install – 20A" to 2.4),
            s("outlet_usb", "USB / Smart Outlet", "NEC 210.52", 55.0, 25.0, 0.3, "outlet", "1-for-1 Swap" to 1.0, "New Install – USB-A/C" to 2.2, "New Install – Smart Wi-Fi" to 2.8, "New Install – TR" to 2.3),
            s("outlet_240", "240V Outlet", "NEC 210.19", 55.0, 170.0, 2.0, "outlet", "NEMA 6-20 (Welder)" to 1.0, "NEMA 14-30 (Dryer)" to 1.1, "NEMA 14-50 (RV/EV)" to 1.2),
            s("outlet_floor", "Floor Outlet", "NEC 314.27(B)", 85.0, 170.0, 2.0, "outlet", "Standard" to 1.0, "With Cover" to 1.2),
            s("outlet_exterior", "Exterior Outlet", "NEC 210.52(E)", 45.0, 43.0, 0.5, "outlet", "1-for-1 Swap" to 1.0, "New – In-Use Cover" to 3.0, "New – WR + In-Use Cover" to 3.4, "New – Recessed" to 4.0),
            s("outlet_countertop", "Countertop Popup Outlet", "NEC 210.52(C)", 120.0, 213.0, 2.5, "outlet", "Standard" to 1.0, "With USB" to 1.2, "Wireless Charger" to 1.5),
            s("outlet_ev", "EV Charger / EVSE", "NEC 625.40", 280.0, 425.0, 5.0, "unit", "NEMA 14-50 Outlet" to 1.0, "Level 2 Hardwire" to 1.5, "Smart EVSE" to 1.8),
        )),
        CatalogCategory("switches", "Switches & Controls", listOf(
            s("switch_single", "Single Pole Switch", "NEC 404.2", 14.0, 21.0, 0.25, "switch", "1-for-1 Swap – Standard" to 1.0, "1-for-1 Swap – Decorator" to 1.05, "1-for-1 Swap – Lighted" to 1.1, "New Location" to 2.0),
            s("switch_3way", "3-Way Switch", "NEC 404.2", 24.0, 34.0, 0.4, "switch", "1-for-1 Swap – Standard" to 1.0, "1-for-1 Swap – Decorator" to 1.1, "New Location" to 2.5),
            s("switch_4way", "4-Way Switch", "NEC 404.2", 35.0, 43.0, 0.5, "switch", "1-for-1 Swap – Standard" to 1.0, "1-for-1 Swap – Decorator" to 1.1, "New Location" to 2.5),
            s("dimmer_single", "Dimmer – Single Pole", "NEC 404.14", 35.0, 30.0, 0.35, "switch", "1-for-1 Swap – Standard" to 1.0, "1-for-1 Swap – LED Compatible" to 1.1, "1-for-1 Swap – Smart/Wi-Fi" to 1.4, "New Location" to 2.1),
            s("dimmer_3way", "Dimmer – 3-Way", "NEC 404.14", 55.0, 43.0, 0.5, "switch", "1-for-1 Swap – Standard" to 1.0, "1-for-1 Swap – Smart/Wi-Fi" to 1.5, "New Location" to 2.5),
            s("switch_smart", "Smart Switch / Scene Controller", "NEC 404.2", 85.0, 64.0, 0.75, "switch", "1-for-1 Swap – Wi-Fi" to 1.0, "1-for-1 Swap – Z-Wave/Zigbee" to 1.15, "1-for-1 Swap – Lutron Caseta" to 1.25, "New Location" to 1.8),
            s("gfci_switch", "GFCI Combo Switch", "NEC 210.8", 45.0, 30.0, 0.35, "switch", "1-for-1 Swap" to 1.0, "New Install" to 2.0),
            s("occupancy_sensor", "Occupancy / Motion Switch", "NEC 404.2", 55.0, 51.0, 0.6, "switch", "1-for-1 Swap – Wall Switch" to 1.0, "New Location – Wall" to 1.8, "New – Ceiling Sensor" to 2.5),
            s("timer_switch", "Timer Switch", "NEC 404.2", 35.0, 30.0, 0.35, "switch", "1-for-1 Swap – Mechanical" to 1.0, "1-for-1 Swap – Digital" to 1.15, "New – Outdoor Intermatic" to 2.2),
        )),
        CatalogCategory("lighting", "Lighting & Fixtures", listOf(
            s("light_ceiling", "Ceiling Light Fixture", "NEC 314.27", 55.0, 64.0, 0.75, "fixture", "Swap on Existing Box" to 1.0, "New Box + Flush Mount" to 2.0, "New Box + Semi-Flush" to 2.1),
            s("light_recessed", "Recessed Lighting (Can)", "NEC 410.116", 45.0, 106.0, 1.25, "fixture", "Remodel/Retrofit" to 1.0, "New Construction" to 0.6, "IC Rated Remodel" to 1.1, "Airtight AT Rated" to 1.15),
            s("light_pendant", "Pendant Light", "NEC 410.36", 80.0, 128.0, 1.5, "fixture", "Swap on Existing Box" to 1.0, "New Location – Standard" to 1.65, "New – Over Island" to 1.85, "New – Heavy/Oversized" to 2.3),
            s("light_chandelier", "Chandelier", "NEC 410.36", 120.0, 213.0, 2.5, "fixture", "Swap on Existing Box" to 1.0, "New – Standard (<50 lbs)" to 1.4, "New – Heavy (50-100 lbs)" to 2.0, "New – Vaulted/High Ceiling" to 2.4),
            s("light_fan", "Ceiling Fan", "NEC 314.27(D)", 100.0, 106.0, 1.25, "fixture", "Swap on Fan-Rated Box" to 1.0, "New Fan-Rated Box + Fan" to 2.0, "With Light Kit – Swap" to 1.1, "Remote/Smart – Swap" to 1.25, "Vaulted Mount – New" to 2.4),
            s("light_track", "Track Lighting", "NEC 410.151", 90.0, 128.0, 1.5, "run", "4 ft" to 1.0, "8 ft" to 1.6, "Flexible/Monorail" to 2.0),
            s("light_undercab", "Under-Cabinet Lighting", "NEC 411.2", 70.0, 128.0, 1.5, "run", "LED Strip (per 4 ft)" to 1.0, "Puck Lights" to 0.9, "Hardwired LED Bar" to 1.3),
            s("light_exterior", "Exterior / Porch Light", "NEC 410.10", 55.0, 64.0, 0.75, "fixture", "Swap on Existing Box" to 1.0, "New – Wall Sconce" to 2.0, "New – Flood/Security" to 2.2, "New – Motion Activated" to 2.4, "New – Post/Pier Mount" to 3.0),
            s("light_vanity", "Bathroom Vanity Light", "NEC 410.10(D)", 60.0, 64.0, 0.75, "fixture", "Swap on Existing Box" to 1.0, "New – Standard Bar" to 2.0, "New – Hollywood/Globe" to 2.1, "New – Lighted Mirror" to 2.5),
            s("exhaust_fan", "Bathroom Exhaust Fan", "NEC 210.11", 80.0, 85.0, 1.0, "unit", "1-for-1 Swap – Standard" to 1.0, "1-for-1 Swap – With Light" to 1.15, "New Cut-In – Standard" to 2.5, "New Cut-In – With Heater" to 2.8, "New Cut-In – High-CFM" to 2.6),
            s("led_retrofit", "LED Retrofit / Recessed Kit", "NEC 410.6", 20.0, 17.0, 0.2, "fixture", "Standard Bulb Swap" to 1.0, "Recessed Retrofit Kit" to 2.5),
            s("landscape_light", "Landscape / Low-Voltage Lighting", "NEC 411.2", 120.0, 213.0, 2.5, "zone", "Low-Voltage Kit" to 1.0, "Hardwired Line Voltage" to 1.8, "Solar Accent" to 0.6),
        )),
        CatalogCategory("safety", "Safety & Detection", listOf(
            s("smoke_detector", "Smoke Detector", "NEC 760.41", 35.0, 34.0, 0.4, "unit", "Battery (Swap/New)" to 1.0, "Hardwired – Swap" to 1.0, "Hardwired + Interconnect – New" to 1.9),
            s("co_detector", "CO Detector", "NEC 760.41", 40.0, 34.0, 0.4, "unit", "Battery (Swap/New)" to 1.0, "Hardwired – Swap" to 1.05, "Combo Smoke/CO – Swap" to 1.15),
            s("smoke_co_combo", "Smoke + CO Combo (Hardwired)", "NEC 210.12", 65.0, 51.0, 0.6, "unit", "Swap Existing" to 1.0, "New – Interconnected" to 1.65, "New – Smart (Nest/Ring)" to 2.0),
            s("arc_fault", "AFCI Protection – Retrofit", "NEC 210.12", 70.0, 106.0, 1.25, "circuit", "Breaker Type" to 1.0, "Outlet Type" to 0.32),
            s("gfci_protection", "GFCI Protection – Retrofit", "NEC 210.8", 38.0, 85.0, 1.0, "circuit", "Breaker Type" to 1.0, "Outlet Type (Feeds Multiple)" to 0.4),
            s("tamper_resistant", "Tamper-Resistant Receptacles", "NEC 406.12", 20.0, 21.0, 0.25, "outlet", "1-for-1 Swap – Standard TR" to 1.0, "1-for-1 Swap – TR + WR" to 1.15),
            s("surge_individual", "Point-of-Use Surge Protection", "NEC 230.67", 30.0, 21.0, 0.25, "unit", "Outlet Type – Swap" to 1.0, "Hardwired" to 3.0),
        )),
        CatalogCategory("hvac_appliance", "HVAC & Appliance Connections", listOf(
            s("ac_disconnect", "AC Disconnect / Whip", "NEC 440.14", 120.0, 213.0, 2.5, "unit", "Non-Fused" to 1.0, "Fused" to 1.2, "60A" to 1.0, "100A" to 1.3),
            s("hvac_circuit", "HVAC Dedicated Circuit", "NEC 440.32", 180.0, 425.0, 5.0, "unit", "Mini Split" to 1.0, "Central AC" to 1.2, "Heat Pump" to 1.2, "Electric Furnace" to 1.4),
            s("dryer_hookup", "Dryer Hookup / Connection", "NEC 220.54", 55.0, 128.0, 1.5, "unit", "3-Wire NEMA 10-30" to 1.0, "4-Wire NEMA 14-30" to 1.0, "Gas Dryer Outlet Only" to 0.6),
            s("range_hookup", "Range / Oven Hookup", "NEC 220.55", 65.0, 128.0, 1.5, "unit", "Freestanding Range" to 1.0, "Wall Oven" to 1.2, "Cooktop + Oven" to 1.5),
            s("water_heater", "Electric Water Heater Circuit", "NEC 422.11", 150.0, 298.0, 3.5, "unit", "Standard Tank" to 1.0, "Tankless/On-Demand" to 1.6),
            s("generator_switch", "Generator Transfer Switch", "NEC 702.6", 450.0, 510.0, 6.0, "unit", "Manual Transfer" to 1.0, "Interlock Kit" to 0.6, "Auto Transfer (ATS)" to 2.2),
            s("generator_inlet", "Generator Inlet Box", "NEC 702.7", 120.0, 213.0, 2.5, "unit", "30A Inlet" to 1.0, "50A Inlet" to 1.2),
            s("pool_equipment", "Pool / Spa Equipment Circuit", "NEC 680.21", 250.0, 510.0, 6.0, "unit", "Pump Motor" to 1.0, "Heater" to 1.3, "Full Equipment Pad" to 2.0),
        )),
        CatalogCategory("smart_home", "Structured Wiring & Smart Home", listOf(
            s("data_panel", "Structured Media / Data Panel", "NEC 800.133", 220.0, 340.0, 4.0, "unit", "Basic" to 1.0, "With Network Switch" to 1.4),
            s("cat6_drop", "Cat6 Home Run (Per Drop)", "NEC 725.41", 60.0, 128.0, 1.5, "drop", "To Patch Panel" to 1.0, "Terminated Both Ends" to 1.1),
            s("coax_drop", "Coax Run (Per Drop)", "NEC 820.133", 35.0, 106.0, 1.25, "drop", "Standard" to 1.0, "RG6 Quad Shield" to 1.1),
            s("doorbell", "Doorbell / Video Doorbell", "NEC 725.41", 60.0, 64.0, 0.75, "unit", "1-for-1 Swap" to 1.0, "New Wired – Ring/Nest" to 2.0, "New Circuit Required" to 3.3),
            s("intercom", "Intercom System", "NEC 725.41", 180.0, 255.0, 3.0, "unit", "Basic Door Intercom" to 1.0, "Multi-Room" to 2.0),
            s("security_prewire", "Security System Pre-Wire", "NEC 760.41", 120.0, 255.0, 3.0, "zone", "Per Zone" to 1.0, "Full House (8 zones)" to 5.0),
            s("whole_audio", "Whole-Home Audio Pre-Wire", "NEC 725.41", 80.0, 170.0, 2.0, "room", "Stereo (2 speakers)" to 1.0, "Surround (5 drops)" to 2.0),
        )),
        CatalogCategory("outdoor", "Outdoor, Garage & Specialty", listOf(
            s("garage_circuit", "Garage Circuit", "NEC 210.52(G)", 140.0, 298.0, 3.5, "circuit", "20A General" to 1.0, "240V Workshop" to 1.4, "EV Charger" to 1.55),
            s("outdoor_subpanel", "Outdoor Sub-Panel (Detached)", "NEC 225.30", 600.0, 850.0, 10.0, "unit", "60A Overhead" to 1.0, "100A Overhead" to 1.3, "100A Underground" to 1.5),
            s("gfci_outdoor", "Outdoor GFCI Outlet", "NEC 210.8(A)(3)", 45.0, 43.0, 0.5, "outlet", "1-for-1 Swap" to 1.0, "New – Wall Mount" to 3.0, "New – Deck Box" to 3.4, "New – In-Ground/Pedestal" to 4.0),
            s("flood_light", "Security / Flood Light", "NEC 410.10", 65.0, 64.0, 0.75, "fixture", "Swap on Existing Box" to 1.0, "New – Motion Flood" to 2.65, "New – Camera + Light" to 3.2, "New – Dusk-to-Dawn" to 2.8),
            s("conduit_run", "Conduit Run (per 10 ft)", "NEC 358.26", 35.0, 64.0, 0.75, "10 ft", "EMT" to 1.0, "PVC Schedule 40" to 0.85, "Rigid/GRC" to 1.4, "Underground Direct Burial" to 1.6),
            s("solar_ready", "Solar / Battery Ready Conduit", "NEC 690.12", 280.0, 510.0, 6.0, "unit", "Panel Conduit Stub" to 1.0, "Full Conduit + Junction" to 1.5),
            s("hot_tub", "Hot Tub / Spa Install", "NEC 680.42", 380.0, 680.0, 8.0, "unit", "Plug-in NEMA 14-50" to 1.0, "Hardwired 240V/50A" to 1.4, "With GFCI + Bonding" to 1.6),
            s("ev_parking", "Driveway / Parking EV Outlet", "NEC 625.40", 300.0, 510.0, 6.0, "unit", "NEMA 14-50 Outdoor" to 1.0, "Level 2 EVSE Pedestal" to 1.6),
            s("exterior_disconnect", "Exterior Emergency Disconnect", "NEC 230.85", 180.0, 255.0, 3.0, "unit", "Meter Main Combo" to 1.0, "Separate Exterior Disconnect" to 1.3),
            s("energy_mgmt", "Energy Management System", "NEC 220.70", 350.0, 425.0, 5.0, "unit", "Basic Load Monitor" to 1.0, "Full Smart Panel Integration" to 2.0),
        )),
        CatalogCategory("pool_water", "Pool, Spa & Water Features", listOf(
            s("pool_bonding", "Pool Equipotential Bonding", "NEC 680.26", 280.0, 595.0, 7.0, "system", "New Pool – Full Bond" to 1.0, "Existing Pool – Remediation" to 1.3, "With Rebar Grid" to 1.5),
            s("pool_grounding", "Pool Equipment Grounding", "NEC 680.6", 120.0, 255.0, 3.0, "system", "Equipment Pad Ground" to 1.0, "Full System Ground + Bond" to 1.8),
            s("pool_pump_circuit", "Pool Pump Motor Circuit", "NEC 680.21", 180.0, 425.0, 5.0, "unit", "120V Single Speed" to 1.0, "240V Single Speed" to 1.2, "240V Variable Speed" to 1.4),
            s("pool_heater_circuit", "Pool / Spa Heater Circuit", "NEC 680.21", 220.0, 510.0, 6.0, "unit", "Gas Heater Outlet" to 0.6, "Electric Heater 240V/50A" to 1.0, "Heat Pump Circuit" to 1.1),
            s("pool_receptacle", "Pool Area Receptacle", "NEC 680.22", 65.0, 213.0, 2.5, "outlet", "6–10 ft from Edge (GFCI)" to 1.0, "10–20 ft from Edge (GFCI)" to 1.0, "Deck Pedestal Box" to 1.5),
            s("pool_light_under", "Underwater Pool Light", "NEC 680.23", 380.0, 510.0, 6.0, "fixture", "120V Niched Fixture" to 1.0, "12V Low-Voltage" to 0.9, "LED Color Retrofit" to 0.8, "New Niche Install" to 1.6),
            s("pool_light_above", "Above-Water / Deck Pool Lighting", "NEC 680.22", 120.0, 255.0, 3.0, "fixture", "Deck Post Light" to 1.0, "Landscape Uplighting" to 1.2, "Step / Hardscape Lights" to 1.4),
            s("pool_disconnect", "Pool Equipment Disconnect", "NEC 680.12", 120.0, 213.0, 2.5, "unit", "Single Equipment" to 1.0, "Full Pad Disconnect" to 1.5),
            s("pool_gfci", "Pool GFCI Breaker Protection", "NEC 680.21(C)", 90.0, 106.0, 1.25, "circuit", "20A Pump Circuit" to 1.0, "With Heater Circuit" to 1.5),
            s("fountain_circuit", "Fountain / Water Feature Circuit", "NEC 680.51", 120.0, 255.0, 3.0, "unit", "Small Fountain (120V)" to 1.0, "Large Feature (240V)" to 1.5, "With Underwater Lighting" to 1.8),
        )),
        CatalogCategory("generator_systems", "Generator & Backup Power Systems", listOf(
            s("generator_install", "Standby Generator Installation", "NEC 702.4", 800.0, 1020.0, 12.0, "unit", "7–12 kW Air-Cooled" to 1.0, "14–20 kW Air-Cooled" to 1.3, "22+ kW Liquid-Cooled" to 1.8),
            s("generator_transfer2", "Generator Transfer Switch", "NEC 702.6", 450.0, 510.0, 6.0, "unit", "Manual – 6 Circuit" to 1.0, "Manual – 10 Circuit" to 1.2, "Interlock Kit" to 0.55, "Automatic (ATS)" to 2.2),
            s("generator_inlet2", "Generator Inlet Box", "NEC 702.7", 120.0, 213.0, 2.5, "unit", "30A / 7500W" to 1.0, "50A / 12500W" to 1.2),
            s("generator_circuit", "Generator Dedicated Circuit / Run", "NEC 702.5", 200.0, 340.0, 4.0, "unit", "From Panel to Inlet" to 1.0, "Long Run (50+ ft)" to 1.5),
            s("battery_storage", "Battery Storage System (e.g. Powerwall)", "NEC 706.20", 1200.0, 850.0, 10.0, "unit", "Single Unit" to 1.0, "Stacked (2+ Units)" to 1.6, "With Gateway/Inverter" to 1.4),
            s("solar_inverter", "Solar Inverter / Disconnect Install", "NEC 690.15", 400.0, 510.0, 6.0, "unit", "String Inverter" to 1.0, "Microinverter System" to 1.4, "Hybrid Inverter (Solar + Battery)" to 1.8),
            s("generator_grounding", "Generator Grounding & Bonding", "NEC 250.35", 80.0, 170.0, 2.0, "unit", "Portable Generator" to 1.0, "Standby – Separately Derived" to 1.8),
        )),
        CatalogCategory("required_outlets", "Required Outlets & Lighting (NEC 210)", listOf(
            s("lighting_stair", "Stairway Lighting Outlet", "NEC 210.70(A)(2)", 55.0, 170.0, 2.0, "outlet", "New Box + Switch" to 1.0, "3-Way Stair Control" to 1.5),
            s("lighting_attic", "Attic / Crawl Space Lighting", "NEC 210.70(A)(3)", 55.0, 213.0, 2.5, "outlet", "Single Fixture + Switch" to 1.0, "Multiple Fixtures" to 1.5),
            s("lighting_basement", "Basement / Utility Room Lighting", "NEC 210.70(A)(3)", 55.0, 170.0, 2.0, "outlet", "Single Fixture + Switch" to 1.0, "Multiple Fixtures" to 1.4),
            s("lighting_garage_req", "Garage Required Lighting Outlet", "NEC 210.70(A)(2)", 55.0, 170.0, 2.0, "outlet", "Ceiling Fixture + Switch" to 1.0, "With Opener Circuit" to 1.4),
            s("lighting_closet", "Closet Lighting", "NEC 410.16", 35.0, 85.0, 1.0, "fixture", "LED Surface Mount (Swap)" to 1.0, "New Box + Fixture" to 1.8),
            s("hvac_outlet_req", "HVAC Service Outlet (Required)", "NEC 210.63", 45.0, 255.0, 3.0, "outlet", "Within 25 ft of Equipment" to 1.0),
            s("panel_outlet_req", "Panel Service Outlet (Required)", "NEC 210.64", 45.0, 213.0, 2.5, "outlet", "Within 50 ft of Panel" to 1.0),
            s("bathroom_circuit", "Bathroom Branch Circuit", "NEC 210.11(C)(3)", 140.0, 340.0, 4.0, "circuit", "Dedicated 20A Bathroom" to 1.0, "Shared Bath (max 2)" to 1.0),
            s("kitchen_sabc", "Kitchen Small Appliance Circuit", "NEC 210.52(B)", 140.0, 340.0, 4.0, "circuit", "First 20A Circuit" to 1.0, "Second 20A Circuit (Required)" to 1.0),
            s("laundry_circuit", "Laundry Branch Circuit", "NEC 210.52(F)", 140.0, 340.0, 4.0, "circuit", "20A Dedicated Laundry" to 1.0),
            s("ev_ready_conduit", "EV-Ready Garage Conduit (Required)", "NEC 210.17", 180.0, 340.0, 4.0, "unit", "Conduit Stub to Panel" to 1.0, "Full Conduit + 40A Circuit" to 1.8),
        )),
        CatalogCategory("inspection_permits", "Permits, Inspections & Load Calc", listOf(
            s("permit_service", "Electrical Permit (Service/Panel)", "NEC 90.4", 0.0, 300.0, 2.0, "permit", "Standard Permit Pull" to 1.0, "Expedited" to 1.5),
            s("permit_general", "Electrical Permit (General Work)", "NEC 90.4", 0.0, 150.0, 1.0, "permit", "Standard" to 1.0, "Expedited" to 1.5),
            s("load_calc", "Load Calculation Service", "NEC 220.82", 0.0, 255.0, 3.0, "service", "Optional Method (220.82)" to 1.0, "Standard Method (220.42)" to 1.2, "With Written Report" to 1.5),
            s("panel_labeling", "Panel Circuit Labeling", "NEC 408.4", 15.0, 128.0, 1.5, "panel", "Existing Panel – Label Only" to 1.0, "Full Circuit Directory + Label" to 1.5),
            s("inspection_final", "Final Electrical Inspection", "NEC 110.3", 0.0, 170.0, 2.0, "service", "Standard Inspection" to 1.0, "Re-Inspection (corrections)" to 0.75),
            s("whole_house_afci", "Whole-House AFCI Upgrade", "NEC 210.12", 600.0, 850.0, 10.0, "system", "10–15 Circuits" to 1.0, "20+ Circuits" to 1.5),
            s("whole_house_gfci", "Whole-House GFCI Upgrade", "NEC 210.8", 400.0, 595.0, 7.0, "system", "Required Locations Only" to 1.0, "Full Home" to 1.5),
            s("whole_house_surge", "Whole-House Surge + AFCI/GFCI Package", "NEC 230.67", 900.0, 1190.0, 14.0, "system", "Standard Package" to 1.0, "With Panel Upgrade" to 1.8),
        )),
    )

    val allServices: List<CatalogService> = categories.flatMap { it.services }

    private val byId: Map<String, CatalogService> = allServices.associateBy { it.id }

    fun service(id: String): CatalogService? = byId[id]
}
