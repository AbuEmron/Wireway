// NEC 2023 Complete Residential Reference
// Every article relevant to residential electrical work
// Plain English summaries + quick answers + AI chat

import React, { useState } from "react";

// ─── COMPLETE NEC 2023 RESIDENTIAL ARTICLES ──────────────────────────────────
export const NEC_RESIDENTIAL = [

  // ── CHAPTER 1 — GENERAL ──────────────────────────────────────────────────
  { art:"90.2",  title:"Scope — What NEC Covers",           cat:"General",
    plain:"The NEC covers all electrical wiring inside buildings — homes, garages, outbuildings, and on the property. It does NOT cover the utility company's wires from the pole to your meter.",
    keyPoints:["Covers all wiring inside and on the property","Does not cover utility company equipment","Local AHJ (inspector) may have amendments that are stricter"],
    newIn2023:false },

  { art:"100",   title:"Definitions",                        cat:"General",
    plain:"Key terms every electrician must know. When the code says 'accessible,' 'approved,' 'dwelling unit,' or 'listed' — these have specific legal meanings that affect how you install.",
    keyPoints:["Accessible = can be reached without removing structure","Approved = acceptable to the AHJ (inspector)","Listed = tested and certified by a testing lab (UL, ETL, etc.)","Dwelling unit = one or more rooms for living including permanent kitchen"],
    newIn2023:false },

  { art:"110.3", title:"Examination, Identification & Installation of Equipment", cat:"General",
    plain:"All electrical equipment must be installed according to its listing and labeling. If a device says 'for indoor use only' — you cannot put it outside. Period.",
    keyPoints:["Install equipment per its listing/labeling — no exceptions","Listed equipment must be used for its intended purpose","Instructions packaged with equipment are part of the listing"],
    newIn2023:false },

  { art:"110.12", title:"Mechanical Execution of Work",      cat:"General",
    plain:"Work must be done in a neat and workmanlike manner. Sloppy wiring, dangling cables, or poor installations can be rejected by an inspector even if technically code compliant.",
    keyPoints:["Work must be neat and workmanlike","Unused openings in boxes and panels must be closed","Cables must be supported and protected"],
    newIn2023:false },

  { art:"110.14", title:"Electrical Connections",            cat:"General",
    plain:"Connections must be made with listed connectors. Aluminum and copper wires cannot be joined without a connector rated for both metals. Wire nuts must be rated for the wire size.",
    keyPoints:["Use only listed connectors","Aluminum and copper require AL/CU rated devices","Do not intermix conductor sizes in wire nuts unless listed for it","Torque terminals to manufacturer spec — use a torque screwdriver"],
    newIn2023:false },

  // ── CHAPTER 2 — WIRING AND PROTECTION ────────────────────────────────────
  { art:"200.6", title:"Identification of Grounded Conductors (Neutral)", cat:"Wiring & Protection",
    plain:"The neutral wire (grounded conductor) must be white or gray, or marked with white tape at terminations. Never use white wire as a hot wire unless re-identified with colored tape.",
    keyPoints:["Neutral = white or gray insulation","White wire used as hot must be re-identified with black/red tape at both ends","In cables, the white is always neutral unless re-identified"],
    newIn2023:false },

  { art:"210.3", title:"Rating of Branch Circuits",          cat:"Branch Circuits",
    plain:"Branch circuit ratings are determined by the overcurrent device (breaker or fuse). A 20-amp circuit needs a 20-amp breaker and 12 AWG wire minimum.",
    keyPoints:["Circuit rating = ampere rating of the OCPD (breaker/fuse)","15A circuit = 14 AWG minimum","20A circuit = 12 AWG minimum","30A circuit = 10 AWG minimum"],
    newIn2023:false },

  { art:"210.8", title:"GFCI Protection — Where Required",  cat:"Branch Circuits",
    plain:"GFCI protection is required anywhere near water or in wet/damp areas. This is one of the most common code violations found during inspections.",
    keyPoints:[
      "Bathrooms — ALL receptacles",
      "Garages — ALL receptacles (including ceiling for garage door openers)",
      "Outdoors — ALL receptacles",
      "Crawl spaces — ALL receptacles",
      "Unfinished basements — ALL receptacles",
      "Kitchen — receptacles within 6 feet of a sink",
      "Laundry areas — ALL receptacles (NEW in 2023)",
      "Boathouses — ALL receptacles",
      "Bathtub/shower areas — ALL receptacles within 6 feet",
      "Sinks — ALL receptacles within 6 feet in non-kitchen areas (NEW in 2023)"
    ],
    newIn2023:true },

  { art:"210.11", title:"Branch Circuit Requirements",       cat:"Branch Circuits",
    plain:"Certain circuits must be dedicated — they cannot serve any other outlets or loads. These are minimum requirements for every home.",
    keyPoints:[
      "Two 20A small appliance circuits required for kitchen/dining — nothing else on these",
      "One 20A laundry circuit — serves laundry area only",
      "One 20A bathroom circuit — serves bathroom receptacles only",
      "Bathroom circuit can serve multiple bathrooms but no other loads"
    ],
    newIn2023:false },

  { art:"210.12", title:"AFCI Protection — Where Required",  cat:"Branch Circuits",
    plain:"Arc-fault protection is required in most living areas of a home. AFCI breakers detect dangerous arcing that regular breakers miss — arcing causes thousands of home fires yearly.",
    keyPoints:[
      "Required in: ALL bedrooms, living rooms, dining rooms, family rooms",
      "Required in: parlors, libraries, dens, sunrooms, recreation rooms",
      "Required in: closets, hallways, laundry areas, and similar rooms",
      "Can use AFCI outlet at first outlet if panel is far away",
      "Not required in bathrooms, garages, or outdoors"
    ],
    newIn2023:false },

  { art:"210.19", title:"Conductors — Minimum Ampacity",     cat:"Branch Circuits",
    plain:"Wire size must match the circuit. Use the right gauge — undersized wire is a fire hazard.",
    keyPoints:["15A circuit = 14 AWG minimum","20A circuit = 12 AWG minimum","30A circuit = 10 AWG minimum","40A circuit = 8 AWG minimum","50A circuit = 6 AWG minimum","Always size wire BEFORE the breaker — wire must handle breaker rating"],
    newIn2023:false },

  { art:"210.23", title:"Permissible Loads on Branch Circuits", cat:"Branch Circuits",
    plain:"How much load can you put on a circuit. The 80% rule — continuous loads cannot exceed 80% of circuit capacity.",
    keyPoints:["15A circuit = max 12A continuous load","20A circuit = max 16A continuous load","Single appliance on 15A circuit = max 12A","Lighting loads = continuous (3+ hours) — use 80% rule"],
    newIn2023:false },

  { art:"210.52", title:"Receptacle Outlets — Where Required in Dwellings", cat:"Branch Circuits",
    plain:"This is the most referenced article in residential work. It tells you exactly where receptacles must go in every room of a house.",
    keyPoints:[
      "General areas: receptacle within 6 feet of any doorway, then every 12 feet along wall",
      "No point on a wall shall be more than 6 feet from a receptacle",
      "Walls 2 feet or wider must have a receptacle",
      "Kitchen countertops: receptacle every 4 feet, within 2 feet of each end",
      "Island/peninsula countertops: at least 1 receptacle for each 9 sq ft",
      "Bathrooms: at least 1 receptacle within 3 feet of each basin",
      "Outdoor: at least 1 front and 1 back, accessible from grade",
      "Garage: at least 1 per car space plus 1 for each 500 sq ft over first 1000",
      "Basement: at least 1 unfinished basement receptacle",
      "Hallways 10+ feet: at least 1 receptacle",
      "Laundry: at least 1 receptacle within 6 feet of laundry equipment"
    ],
    newIn2023:false },

  { art:"210.63", title:"HVAC Receptacle Outlet",            cat:"Branch Circuits",
    plain:"A 125V receptacle must be installed within 25 feet of heating and cooling equipment in attics and crawl spaces for service technicians.",
    keyPoints:["Required in attic or crawl space where HVAC equipment is located","Must be within 25 feet of the equipment","Must be on same level as equipment","Protects service technicians from having to use extension cords"],
    newIn2023:false },

  { art:"210.64", title:"Electrical Service Areas Receptacle", cat:"Branch Circuits",
    plain:"A receptacle must be installed in electrical service areas like electrical rooms and near service equipment.",
    keyPoints:["Required within 50 feet of electrical service equipment","Accessible without moving equipment","Helps electricians and inspectors without running extension cords"],
    newIn2023:true },

  { art:"215.2", title:"Feeder Minimum Ampacity",            cat:"Feeders",
    plain:"Feeder wires to a subpanel must be large enough to handle the load. Size based on 100% of non-continuous + 125% of continuous loads.",
    keyPoints:["Non-continuous loads = 100% of load","Continuous loads (3+ hours) = 125% of load","Feeder must also handle all branch circuit loads it supplies","Check voltage drop on long runs — max 3% recommended"],
    newIn2023:false },

  { art:"220.40", title:"General Load Calculations",         cat:"Load Calculations",
    plain:"How to add up all the loads in a house to determine what size service and feeders are needed. Every residential job needs a load calculation.",
    keyPoints:["General lighting: 3 VA per square foot of living space","Small appliance circuits: 1,500 VA each (minimum 2)","Laundry circuit: 1,500 VA","Apply demand factors for large appliances (washer, dryer, range)"],
    newIn2023:false },

  { art:"220.82", title:"Optional Load Calculation Method",  cat:"Load Calculations",
    plain:"A simplified method for calculating residential service loads. Often used for service upgrades to existing homes.",
    keyPoints:["First 10 kVA at 100%, remainder at 40%","Add heating OR cooling (largest load) at 100%","Add all other appliances at 100%","Simpler than standard method — preferred for service upgrades"],
    newIn2023:false },

  { art:"225.30", title:"Number of Supplies to Buildings",   cat:"Feeders",
    plain:"A detached garage, shed, or other building on your property can only have one feeder from the house — unless exceptions apply (emergency, fire pumps, etc.).",
    keyPoints:["Only one feeder to a detached structure","Disconnect required at the detached building","Grounding electrode required at detached building","EGC runs with feeder — no separate grounding at detached structure with metallic feeder conduit"],
    newIn2023:false },

  { art:"225.41", title:"Outside Emergency Disconnect",      cat:"Service",
    plain:"NEW in 2023 — feeders supplying one- and two-family dwellings must have an exterior emergency disconnect so first responders can cut power from outside.",
    keyPoints:["NEW 2023 REQUIREMENT","Exterior disconnect required for feeder-supplied homes","Mirrors 230.85 requirement for service-supplied homes","Allows firefighters to shut off power from outside the building"],
    newIn2023:true },

  { art:"230.2",  title:"Number of Services",                cat:"Service",
    plain:"A building can only have one electrical service unless exceptions apply. Multiple services are allowed for fire pumps, emergency systems, or very large buildings.",
    keyPoints:["One service per building — standard rule","Exceptions: emergency systems, fire pumps, capacity needs","Special permission from AHJ required for additional services"],
    newIn2023:false },

  { art:"230.6",  title:"Conductors Considered Outside a Building", cat:"Service",
    plain:"Service conductors running inside conduit encased in 2+ inches of concrete are considered outside the building — important for underground service runs.",
    keyPoints:["Conductors in 2\" concrete = considered outside building","Allows service conductors to run inside building in conduit","Important for underground service entrance planning"],
    newIn2023:false },

  { art:"230.23", title:"Size and Rating of Service Conductors", cat:"Service",
    plain:"Service conductors must be large enough for the calculated load. Minimum 100A for single-family dwellings.",
    keyPoints:["Minimum 100A service for single-family dwellings","Must handle calculated load per Article 220","Copper or aluminum conductors both acceptable","Aluminum service conductors are very common and acceptable"],
    newIn2023:false },

  { art:"230.24", title:"Clearances for Overhead Service",   cat:"Service",
    plain:"Overhead service wires must clear roofs, driveways, and walkways by specific distances for safety.",
    keyPoints:["10 feet above finished grade at service entrance","12 feet above residential driveways","18 feet above public roads","3 feet above roof (8 feet where subject to pedestrian/vehicle traffic)","Exception: 18-inch clearance for roof with 4:12 pitch or greater"],
    newIn2023:false },

  { art:"230.42", title:"Minimum Size and Rating of Service Entrance Conductors", cat:"Service",
    plain:"The wires coming into your panel from the meter must be sized for the load. Never undersize service entrance conductors.",
    keyPoints:["Size per load calculation Article 220","Minimum 100A for dwelling units","100A service = 4 AWG copper or 2 AWG aluminum minimum","200A service = 2/0 copper or 4/0 aluminum minimum"],
    newIn2023:false },

  { art:"230.54", title:"Overhead Service Locations",        cat:"Service",
    plain:"Where the service wires attach to the building has specific rules to prevent water from running down the wires into the service entrance.",
    keyPoints:["Service head must be above service entrance conductors","Point of attachment below service head","Drip loop required — wires must curve down before entering service head","Raintight service head required"],
    newIn2023:false },

  { art:"230.67", title:"Surge Protection Required (NEW 2023)", cat:"Service",
    plain:"NEW in 2023 — every new service must have a surge protective device (SPD) installed at the service entrance. Protects appliances from voltage spikes.",
    keyPoints:["NEW 2023 REQUIREMENT — applies to all new services","Type 1 or Type 2 SPD required","Installed at or adjacent to service equipment","Whole-home surge protection is now code — not optional"],
    newIn2023:true },

  { art:"230.70", title:"Service Disconnecting Means — Location", cat:"Service",
    plain:"The main breaker or disconnect must be as close as possible to where the service wires enter the building. Usually on the outside wall or just inside.",
    keyPoints:["At nearest point of entrance of service conductors","Inside or outside building","Must be readily accessible","Not in bathrooms, coat closets, or stairways"],
    newIn2023:false },

  { art:"230.79", title:"Rating of Service Disconnect",      cat:"Service",
    plain:"The service disconnect must be rated for the load. Single-family homes require minimum 100A service.",
    keyPoints:["Minimum 100A for single-family dwelling","One main disconnect or up to six disconnects","Fused disconnect or circuit breaker acceptable","Must be rated for available fault current"],
    newIn2023:false },

  { art:"230.85", title:"Emergency Disconnects (NEW 2023)",  cat:"Service",
    plain:"NEW in 2023 — all new services to one- and two-family homes must have a clearly marked exterior emergency disconnect so firefighters can shut power off from outside.",
    keyPoints:["NEW 2023 REQUIREMENT","Exterior emergency disconnect required on all new services","Must be marked 'EMERGENCY DISCONNECT'","Allows first responders to de-energize building from outside","Can be the meter main, exterior disconnect, or other means"],
    newIn2023:true },

  // ── CHAPTER 2 — GROUNDING ─────────────────────────────────────────────────
  { art:"250.4",  title:"General Requirements for Grounding", cat:"Grounding & Bonding",
    plain:"Why we ground electrical systems — to limit voltage from lightning and utility surges, and to ensure overcurrent devices operate during ground faults.",
    keyPoints:["Grounding limits voltage to earth during surges","Equipment grounding provides fault current path to trip breakers","System grounding stabilizes voltage to earth","Bonding ensures all metal parts are at same potential"],
    newIn2023:false },

  { art:"250.20", title:"Alternating-Current Systems to be Grounded", cat:"Grounding & Bonding",
    plain:"120/240V residential systems must be grounded. The neutral wire is connected to earth at the service.",
    keyPoints:["120/240V single-phase systems must be grounded","Grounded at service equipment only","Neutral bonded to ground at service panel only — NOT at subpanels","Separate neutral and ground bars in subpanels"],
    newIn2023:false },

  { art:"250.24", title:"Grounding Service-Supplied Systems", cat:"Grounding & Bonding",
    plain:"At the main service panel, the neutral and ground are bonded together. This is the only place they connect in the system.",
    keyPoints:["Neutral-to-ground bond at main service panel only","Main bonding jumper connects neutral bar to panel enclosure","Grounding electrode conductor runs from neutral bar to ground rods","Never bond neutral to ground at a subpanel"],
    newIn2023:false },

  { art:"250.28", title:"Main Bonding Jumper",               cat:"Grounding & Bonding",
    plain:"The wire or strap that connects the neutral bus to the panel cabinet in the main service panel. Required in main panels — NOT in subpanels.",
    keyPoints:["Required in main service panel only","Connects neutral bus bar to panel enclosure","Sized per Table 250.66 based on service conductor size","Green screw in most residential panels IS the main bonding jumper"],
    newIn2023:false },

  { art:"250.50", title:"Grounding Electrode System",        cat:"Grounding & Bonding",
    plain:"All grounding electrodes present at a building must be connected together. You cannot use just one if multiple exist.",
    keyPoints:["All electrodes must be bonded together","Includes: ground rods, water pipe, building steel, concrete-encased (Ufer)","Metal water pipe if 10+ feet in contact with earth","Concrete-encased electrode (Ufer) is most effective"],
    newIn2023:false },

  { art:"250.52", title:"Grounding Electrodes",              cat:"Grounding & Bonding",
    plain:"What qualifies as a grounding electrode — ground rods, water pipes, building steel, Ufer grounds, etc.",
    keyPoints:["Metal underground water pipe (10+ feet in earth)","Metal building frame","Concrete-encased electrode (Ufer) — 20+ feet of rebar or #4 wire in concrete","Ground rings — #2 AWG bare copper around perimeter","Ground rods — 8 feet minimum driven into earth","Ground plates — 2 sq ft minimum surface area"],
    newIn2023:false },

  { art:"250.53", title:"Grounding Electrode System Installation", cat:"Grounding & Bonding",
    plain:"How to install ground rods and the grounding electrode system. Two rods required if one rod resistance exceeds 25 ohms.",
    keyPoints:["Ground rods must be 8 feet long minimum","Drive full length into ground","If full depth not possible due to rock — angle or bury horizontally","Single rod = supplement with second rod unless resistance ≤ 25 ohms","Two rods must be at least 6 feet apart"],
    newIn2023:false },

  { art:"250.64", title:"Grounding Electrode Conductor Installation", cat:"Grounding & Bonding",
    plain:"The wire from the panel to the ground rods. Must be protected from damage and run continuously without splices (mostly).",
    keyPoints:["Must be protected from physical damage","No splices — run continuously (except at bus bars)","Can run along surface of building — not required to be in conduit (but must be protected)","Aluminum GEC cannot be within 18 inches of earth"],
    newIn2023:false },

  { art:"250.66", title:"Size of Alternating-Current Grounding Electrode Conductor", cat:"Grounding & Bonding",
    plain:"Table tells you what size wire to use from the panel to the ground rods, based on the size of your service conductors.",
    keyPoints:["Size based on largest service entrance conductor","100A service (4 AWG copper) = 8 AWG copper GEC","200A service (2/0 copper) = 4 AWG copper GEC","Aluminum GEC can be used — see Table 250.66 for sizing"],
    newIn2023:false },

  { art:"250.118", title:"Types of Equipment Grounding Conductors", cat:"Grounding & Bonding",
    plain:"What counts as a valid equipment grounding conductor — the green wire, bare copper, metal conduit, etc.",
    keyPoints:["Bare copper wire","Green insulated wire","Green wire with yellow stripe","Metal conduit (EMT, RMC, IMC) — counts as EGC","Metal cable armor (MC, AC cable)","Conduit fittings must be listed for this purpose"],
    newIn2023:false },

  { art:"250.119", title:"Identification of Equipment Grounding Conductors", cat:"Grounding & Bonding",
    plain:"Ground wires must be green, bare, or green with yellow stripes. Never use green for anything else.",
    keyPoints:["Green, bare, or green/yellow = equipment ground only","Cannot use green wire as a hot or neutral","Sizes 6 AWG and smaller must be green or bare","Sizes 4 AWG and larger can be re-identified with green tape"],
    newIn2023:false },

  { art:"250.122", title:"Size of Equipment Grounding Conductors", cat:"Grounding & Bonding",
    plain:"What size green wire to include with each circuit based on the breaker size.",
    keyPoints:["15A breaker = 14 AWG EGC","20A breaker = 12 AWG EGC","30A breaker = 10 AWG EGC","40-60A breaker = 10 AWG EGC","100A breaker = 8 AWG EGC","200A breaker = 6 AWG EGC","EGC never needs to be larger than circuit conductors"],
    newIn2023:false },

  { art:"250.148", title:"Continuity and Attachment of EGC in Boxes", cat:"Grounding & Bonding",
    plain:"All ground wires in a box must be connected together and to the box (if metal). Don't leave grounds disconnected.",
    keyPoints:["All EGCs in a box must be spliced together","Connected to metal box with green screw or ground clip","Devices connected to EGC at device (screw or clip)","Pigtail from ground bundle to box and to device"],
    newIn2023:false },

  // ── CHAPTER 3 — WIRING METHODS ──────────────────────────────────────────
  { art:"300.4", title:"Protection Against Physical Damage", cat:"Wiring Methods",
    plain:"Wires inside walls must be protected from nails and screws. Steel nail plates are required where wires are within 1.25 inches of the surface.",
    keyPoints:["1.25-inch rule — protect wires closer than 1.25\" from edge of stud/joist","Steel nail plates minimum 1/16\" thick","Required where nails or screws could hit wire","Applies to NM cable, THHN in walls, all wiring methods"],
    newIn2023:false },

  { art:"300.5", title:"Underground Installations",          cat:"Wiring Methods",
    plain:"Minimum burial depths for underground wiring. Deeper burial = more protection = some depth requirements can be reduced.",
    keyPoints:["Direct buried cable = 24 inches","PVC conduit = 18 inches","RMC/IMC = 6 inches","Under concrete slab (with RMC) = 6 inches","Under driveways = 24 inches (increase depth)","Under 4-inch concrete that is a floor = 4 inches"],
    newIn2023:false },

  { art:"300.11", title:"Securing and Supporting",           cat:"Wiring Methods",
    plain:"Wires and conduit must be properly supported. You cannot hang wires from ceiling grid T-bars or other systems not designed for electrical support.",
    keyPoints:["Support independently — not on ceiling grid, pipes, HVAC ducts","Cables and raceways must have proper supports","Support spacing depends on wiring method","Secure within 12 inches of each box for NM cable"],
    newIn2023:false },

  { art:"300.14", title:"Length of Free Conductors at Outlets", cat:"Wiring Methods",
    plain:"Leave enough wire at every box to make connections. Minimum 6 inches sticking out, with at least 3 inches outside the box.",
    keyPoints:["Minimum 6 inches of free conductor at each outlet","At least 3 inches must extend outside the box","Applies to all wiring methods","Allows proper connections without straining wires"],
    newIn2023:false },

  { art:"300.15", title:"Boxes, Conduit Bodies, or Fittings", cat:"Wiring Methods",
    plain:"Every splice, connection, or termination must be in a box. No splices in walls — everything goes in a box with a cover.",
    keyPoints:["All splices must be in an accessible box","Box must have a cover","Conduit bodies allowed for splices if large enough","No buried splices — all boxes must be accessible"],
    newIn2023:false },

  { art:"314.16", title:"Number of Conductors in Outlet Boxes", cat:"Wiring Methods",
    plain:"Boxes can only hold so many wires. Add up the wire counts and compare to the box's cubic-inch rating — overloaded boxes are a fire hazard.",
    keyPoints:["Each conductor = 1 unit (based on largest wire in box)","Each device (switch/outlet) = 2 units","All ground wires together = 1 unit","Cable clamps = 1 unit total","Box must have enough cubic inches for all units"],
    newIn2023:false },

  { art:"314.17", title:"Conductors Entering Boxes",         cat:"Wiring Methods",
    plain:"Wires entering boxes must be protected from damage at the entry point. NM cable needs to be clamped.",
    keyPoints:["All conductors entering boxes must be protected","NM cable requires clamp within 12 inches of metal box","Plastic boxes have built-in clamps","Conduit and cable must be secured to box"],
    newIn2023:false },

  { art:"314.20", title:"In-Wall or In-Ceiling Boxes",       cat:"Wiring Methods",
    plain:"Boxes in drywall must be flush with the surface — not recessed. A gap larger than 1/8 inch is a code violation.",
    keyPoints:["Box front must be flush with finished surface","Maximum 1/8-inch setback in combustible materials","No setback allowed in non-combustible materials (tile, concrete)","Box extenders available to fix recessed boxes"],
    newIn2023:false },

  { art:"314.27", title:"Outlet Boxes for Ceiling Fans",     cat:"Wiring Methods",
    plain:"Regular light fixture boxes CANNOT support ceiling fans. You need a fan-rated box that handles the weight and movement.",
    keyPoints:["Standard light boxes NOT rated for fans","Fan-rated boxes required — marked for fan support","Supports fixtures up to 70 lbs if marked","Old work fan boxes available for retrofits","Brace bars available to install between joists"],
    newIn2023:false },

  { art:"334.10", title:"NM Cable — Uses Permitted",         cat:"Wiring Methods",
    plain:"Romex (NM cable) is the most common residential wiring method. Here's where it can be used.",
    keyPoints:["Permitted in dry locations in residential","Not permitted in commercial buildings 3+ stories","Not permitted in wet or damp locations","Not permitted where exposed to physical damage","Must be protected where passing through floors"],
    newIn2023:false },

  { art:"334.15", title:"NM Cable — Protection and Securing", cat:"Wiring Methods",
    plain:"How to properly run and protect Romex cable in a home.",
    keyPoints:["Staple within 12 inches of each box","Staple every 4.5 feet maximum","Protect with conduit or nail plate where within 1.25\" of surface","Cannot run through air plenums","Protect where exposed to physical damage"],
    newIn2023:false },

  { art:"334.80", title:"NM Cable — Ampacity",               cat:"Wiring Methods",
    plain:"When more than two NM cables are bundled together, the ampacity must be derated. Bundled cables can overheat.",
    keyPoints:["3-6 bundled cables = 80% ampacity","7-9 bundled cables = 70% ampacity","If derated, may need to upsize wire","Applies when bundled more than 24 inches","Common in panel areas where cables bunch together"],
    newIn2023:false },

  // ── CHAPTER 4 — EQUIPMENT ────────────────────────────────────────────────
  { art:"400.8", title:"Flexible Cords — Uses Not Permitted", cat:"Equipment",
    plain:"Extension cords and flexible cords cannot be permanently installed. They are temporary — not a substitute for permanent wiring.",
    keyPoints:["Cannot run through walls, ceilings, floors","Cannot be attached to building surfaces permanently","Cannot run under carpets or rugs","Cannot substitute for permanent wiring","Must be visible and accessible at all times"],
    newIn2023:false },

  { art:"404.2", title:"Switches — Requirements",            cat:"Equipment",
    plain:"Switches control the hot wire only — never the neutral. A neutral must be available in switch boxes for smart switches.",
    keyPoints:["Switch must be in the ungrounded (hot) conductor only","Neutral required to be present in switch box (for smart switches)","Switch must be grounded","3-way switches for multi-location control","4-way switches for 3+ location control"],
    newIn2023:false },

  { art:"404.9", title:"Switches — Grounding",               cat:"Equipment",
    plain:"All switches must be grounded. Metal yoke switches get grounded through the yoke if properly installed with screws.",
    keyPoints:["All switches must be grounded","Can be through metal yoke if correctly installed with screws","Plastic-strap switches must have ground wire to green screw","Self-grounding clip counts if properly installed"],
    newIn2023:false },

  { art:"406.4", title:"Receptacle Replacement",             cat:"Equipment",
    plain:"When replacing an outlet, you must upgrade to GFCI if GFCI is required in that location. Cannot replace with same non-GFCI outlet in a GFCI-required location.",
    keyPoints:["Replace with same type — UNLESS GFCI required","GFCI required locations must get GFCI when replacing","Ungrounded circuits: replace with GFCI outlet (no ground needed)","GFCI outlet protects downstream ungrounded outlets — label 'No Equipment Ground'"],
    newIn2023:false },

  { art:"406.9", title:"Receptacles in Wet and Damp Locations", cat:"Equipment",
    plain:"Outdoor and wet location outlets need special covers. In-use covers required where cord is plugged in during rain.",
    keyPoints:["Wet locations = weatherproof while cord IS plugged in (in-use cover)","Damp locations = weatherproof when cord NOT plugged in","In-use covers = flip-up style that covers even with cord","'Extra duty' in-use covers required for outdoor outlets since 2008 NEC"],
    newIn2023:false },

  { art:"406.12", title:"Tamper-Resistant Receptacles",      cat:"Equipment",
    plain:"All 15A and 20A outlets in a home must be tamper-resistant. The shutters inside prevent children from inserting objects.",
    keyPoints:["ALL 15A and 20A receptacles in dwelling units must be tamper-resistant","Applies to all rooms — including garage, basement, outdoors","Tamper-resistant = spring-loaded shutters requiring simultaneous pressure","Exception: receptacles more than 5.5 feet above floor"],
    newIn2023:false },

  { art:"408.4", title:"Circuit Directory",                  cat:"Equipment",
    plain:"Every panel must have a written directory identifying what each breaker controls. 'MISC' or blank is not acceptable.",
    keyPoints:["Every circuit must be identified on directory","Typed or legible handwriting acceptable","Must accurately describe the load — not just 'bedroom' but 'master bedroom lights & outlets'","Directory must be inside panel door"],
    newIn2023:false },

  { art:"408.7", title:"Unused Openings in Panels",          cat:"Equipment",
    plain:"All unused knockouts and breaker spaces in panels must be closed. Prevents contact with live parts and keeps pests out.",
    keyPoints:["Close all unused openings","Blank panel fillers required for empty breaker spaces","Knockout seals for unused conduit entries","Prevents contact with energized parts and pest entry"],
    newIn2023:false },

  { art:"410.10", title:"Luminaires in Specific Locations",  cat:"Lighting",
    plain:"Light fixtures must be rated for their environment. Bathroom shower fixtures need to be rated for wet locations.",
    keyPoints:["Dry locations = any standard fixture","Damp locations = rated for damp or wet","Wet locations = rated for wet only","Shower/tub zone = within 3 feet horizontally and 8 feet vertically of water","Closets = specific rules for fixture placement to prevent fire"],
    newIn2023:false },

  { art:"410.16", title:"Luminaires in Clothes Closets",     cat:"Lighting",
    plain:"Closet lights have specific rules to prevent fires from clothing touching hot bulbs. LED fixtures have the most flexibility.",
    keyPoints:["Surface incandescent = not permitted","Incandescent with open lamp = not permitted","Surface fluorescent/LED = 12 inches from storage area","Recessed with enclosed lamp = 6 inches from storage","Recessed LED = 6 inches from storage area","Storage area = 12 inches deep from any wall/shelf"],
    newIn2023:false },

  { art:"410.116", title:"Recessed Luminaires",              cat:"Lighting",
    plain:"Recessed lights have specific rules for insulation contact. IC-rated fixtures can touch insulation — non-IC cannot.",
    keyPoints:["IC-rated = can be in contact with insulation","Non-IC = must have 3-inch clearance from insulation","Thermal protection required on all recessed fixtures","Fixtures must be installed per their listing","Use IC-AT rated fixtures in insulated ceilings"],
    newIn2023:false },

  { art:"422.11", title:"Overcurrent Protection for Appliances", cat:"Equipment",
    plain:"Appliances must be protected by overcurrent devices sized appropriately. Most residential appliances are cord-and-plug connected.",
    keyPoints:["Cord-and-plug appliances protected by branch circuit OCPD","Permanently connected appliances need individual protection if over 13.3A on 20A circuit","Space heaters, water heaters = dedicated circuits","Dishwasher = dedicated 20A circuit recommended"],
    newIn2023:false },

  { art:"424.3", title:"Electric Space Heating — Branch Circuits", cat:"Equipment",
    plain:"Electric heaters require properly sized circuits. Heaters are continuous loads — size at 125%.",
    keyPoints:["Heaters are continuous loads — size circuit at 125% of heater rating","240V baseboard heaters need dedicated 240V circuit","Thermostat must be rated for line voltage if line-voltage type","Cannot exceed 80% of circuit capacity for continuous load"],
    newIn2023:false },

  { art:"430.22", title:"Motor Branch Circuit Conductors",   cat:"Equipment",
    plain:"Wires to motors must be sized at 125% of the motor's full load current — not just rated load.",
    keyPoints:["Conductors = 125% of motor FLA minimum","Use motor nameplate FLA — not HP alone","Sizing from NEC Table 430.248 if nameplate unavailable","Common in: HVAC, pumps, garbage disposals, exhaust fans"],
    newIn2023:false },

  { art:"440.62", title:"Room A/C Units — Disconnecting Means", cat:"Equipment",
    plain:"Window and through-wall A/C units need a way to disconnect. The attachment plug on cord-and-plug units counts as the disconnect if within sight.",
    keyPoints:["Cord-and-plug A/C = plug is the disconnect if within sight","Within sight = visible and not more than 50 feet","Central A/C = disconnect required within sight of unit","Lockable disconnect required if not within sight"],
    newIn2023:false },

  // ── CHAPTER 5 — SPECIAL OCCUPANCIES ─────────────────────────────────────
  { art:"550.10", title:"Mobile/Manufactured Homes — Service", cat:"Special Occupancies",
    plain:"Mobile homes need a service connection at the lot with a disconnecting means.",
    keyPoints:["Minimum 100A service to mobile home","Disconnect required within sight of mobile home","Mobile home panel inside home — not the service","Underground or overhead service to pedestal or post"],
    newIn2023:false },

  // ── CHAPTER 6 — SPECIAL EQUIPMENT ────────────────────────────────────────
  { art:"550.25", title:"Arc-Fault Circuit-Interrupter Protection", cat:"Special Occupancies",
    plain:"Mobile and manufactured homes require AFCI protection in the same locations as stick-built homes.",
    keyPoints:["Same AFCI requirements as site-built homes","All living areas, bedrooms, hallways","Required at panel or first outlet","Must be listed for mobile home use"],
    newIn2023:false },

  { art:"625.40", title:"Electric Vehicle Charging — Branch Circuit", cat:"Special Equipment",
    plain:"EV chargers need dedicated circuits. Level 2 chargers (240V) are the standard home charger.",
    keyPoints:["Dedicated branch circuit required","Level 1 (120V): standard outlet, 12-16 hours charge","Level 2 (240V): 40-50A circuit, 6 AWG, 4-8 hour charge","GFCI protection required for EVSE","Future-proof: install 60A circuit even if 40A charger now"],
    newIn2023:false },

  { art:"680.21", title:"Pools — Wiring Methods",            cat:"Special Equipment",
    plain:"Electrical near swimming pools has very strict rules to prevent electrocution. Specialized equipment and installation required.",
    keyPoints:["No NM cable within 5 feet of pool","No overhead wiring within 10 feet of pool","All wiring near pool must be in conduit","GFCI required for all receptacles within 20 feet of pool","Underwater lights must be low voltage or specifically listed"],
    newIn2023:false },

  { art:"680.26", title:"Pools — Equipotential Bonding",     cat:"Special Equipment",
    plain:"All metal near a pool must be bonded together to prevent dangerous voltage differences. This is separate from grounding.",
    keyPoints:["Bond all metal parts of pool — water, shell, equipment","8 AWG solid copper bonding conductor minimum","Pump motors, light housings, ladders, rails all bonded","Water itself must be bonded via conductive element","Bonding ≠ grounding — both required"],
    newIn2023:false },

  { art:"690.11", title:"Solar PV — Arc-Fault Protection",   cat:"Special Equipment",
    plain:"Solar panel systems require arc-fault protection on DC circuits. Roof fires from PV arc faults are a real hazard.",
    keyPoints:["DC arc-fault protection required","Rapid shutdown required for roof-mounted systems","Shutdown within 1 foot of array boundary","Must deenergize to ≤80V within 30 seconds","Required on all new PV installations"],
    newIn2023:false },

  { art:"695.6", title:"Fire Pumps — Power Supply",          cat:"Special Equipment",
    plain:"If a home has a fire pump, it needs a reliable power supply that won't be interrupted when the building power fails.",
    keyPoints:["Fire pump needs dedicated power supply","Cannot be on same service as other loads if interruption possible","Transfer switch required if dual supply","Must be sized for locked-rotor current of motor"],
    newIn2023:false },

  // ── CHAPTER 7 — SPECIAL CONDITIONS ───────────────────────────────────────
  { art:"700.16", title:"Emergency Lighting",                cat:"Special Conditions",
    plain:"Emergency lighting must come on automatically when normal power fails. Required in larger homes with specific occupancy requirements.",
    keyPoints:["Must activate automatically on power loss","Battery backup minimum 90 minutes","Minimum 1 footcandle at floor level along egress path","Required in assembly areas, hallways in larger buildings"],
    newIn2023:false },

  { art:"702.12", title:"Optional Standby Systems — Transfer Equipment", cat:"Special Conditions",
    plain:"Home generators must have transfer switches to prevent backfeed to the utility — which can kill utility workers.",
    keyPoints:["Transfer switch REQUIRED — no direct connections","Prevents backfeed to utility (protects lineworkers)","Interlock kits acceptable alternative to full transfer switch","Generator must be grounded per NEC 250","Never parallel generator with utility power without approved equipment"],
    newIn2023:false },

  { art:"705.12", title:"Solar PV — Connection to Grid",     cat:"Special Conditions",
    plain:"Where solar connects to your panel. The 120% rule limits how large a solar connection can be based on panel bus rating.",
    keyPoints:["Load-side connection cannot exceed 120% of panel bus rating","Example: 200A panel bus = max 240A total (utility 200A + solar 40A breaker)","Supply-side connection (before main breaker) has no 120% limit","Utility interconnect agreement required","Production meter and disconnect required"],
    newIn2023:false },

  // ── ANNEX ─────────────────────────────────────────────────────────────────
  { art:"Table 250.66", title:"Grounding Electrode Conductor Size Table", cat:"Tables & Calculations",
    plain:"Use this table to size the wire from your panel to the ground rods, based on your service entrance conductor size.",
    keyPoints:["Service conductors 2 AWG or smaller copper = 8 AWG GEC","Service conductors 1/0–3/0 copper = 4 AWG GEC","Service conductors 4/0 or larger copper = 2 AWG GEC","Service conductors 2/0 or smaller aluminum = 6 AWG GEC","Service conductors 4/0 or larger aluminum = 4 AWG GEC"],
    newIn2023:false },

  { art:"Table 310.12", title:"Conductor Sizing for Dwelling Units", cat:"Tables & Calculations",
    plain:"Simplified wire sizing table for residential work. Use this for standard residential circuits.",
    keyPoints:["15A circuit = 14 AWG copper","20A circuit = 12 AWG copper","30A circuit = 10 AWG copper","40A circuit = 8 AWG copper","50A circuit = 6 AWG copper","Service entrance: 100A = 4 AWG Cu or 2 AWG Al, 200A = 2/0 Cu or 4/0 Al"],
    newIn2023:false },

  { art:"Table 314.16(A)", title:"Metal Box Fill Capacity", cat:"Tables & Calculations",
    plain:"How many wires fit in standard metal boxes. Count your conductors and compare to the box size.",
    keyPoints:["4x4 square box (1.5 deep) = 21 cu in = six 14 AWG or five 12 AWG","Single gang plastic (18 cu in) = six 14 AWG or five 12 AWG","Double gang plastic (32 cu in) = ten 14 AWG or nine 12 AWG","Count: each wire=1, each device=2, all grounds=1, clamps=1"],
    newIn2023:false },

  { art:"Annex D", title:"Load Calculation Examples",       cat:"Tables & Calculations",
    plain:"Sample load calculations for residential services. Use these examples to learn how to size services and feeders.",
    keyPoints:["Shows step-by-step 100A and 200A service calculations","Includes general lighting, small appliance, laundry loads","Shows how to apply demand factors","Optional method (220.82) usually gives smaller service size"],
    newIn2023:false },
];

// ─── QUICK ANSWER LOOKUP ──────────────────────────────────────────────────────
export const QUICK_ANSWERS = [
  { q:"Where is GFCI required?",          icon:"💧",
    a:"Bathrooms, garages, outdoors, crawl spaces, unfinished basements, kitchen within 6ft of sink, laundry areas, boathouses, near bathtubs/showers, and near all sinks (2023 update). See §210.8." },
  { q:"Where is AFCI required?",           icon:"⚡",
    a:"All bedrooms, living rooms, dining rooms, family rooms, dens, libraries, sunrooms, hallways, closets, laundry areas, and similar rooms. Not required in bathrooms or garages. See §210.12." },
  { q:"How far apart do outlets need to be?", icon:"🔌",
    a:"No point on a wall shall be more than 6 feet from an outlet. So outlets every 12 feet max. Within 6 feet of any doorway. Walls 2 feet wide or more need an outlet. See §210.52." },
  { q:"What wire size for a 20-amp circuit?", icon:"🔧",
    a:"12 AWG minimum for a 20-amp circuit. Never use 14 AWG on a 20-amp breaker. See §210.19 and Table 310.12." },
  { q:"What wire size for a 15-amp circuit?", icon:"🔧",
    a:"14 AWG minimum for a 15-amp circuit. See §210.19 and Table 310.12." },
  { q:"Do I need a neutral in a switch box?", icon:"💡",
    a:"Yes — NEC 2011+ requires a neutral (or two switched conductors) to be accessible at switch locations for smart switches. See §404.2." },
  { q:"How deep for underground wire?",    icon:"⛏️",
    a:"Direct buried cable = 24 inches. PVC conduit = 18 inches. RMC/IMC = 6 inches. Under 4-inch concrete slab = 4 inches with RMC. See §300.5." },
  { q:"Can I splice wires in a wall?",     icon:"🚫",
    a:"No. All splices must be in accessible junction boxes with covers. No buried splices allowed. See §300.15." },
  { q:"What size service for a house?",   icon:"🏠",
    a:"Minimum 100-amp service for single-family dwellings. Most new homes use 200-amp. Calculate actual load per Article 220 to confirm. See §230.79." },
  { q:"Do all outlets need to be tamper resistant?", icon:"🔒",
    a:"Yes — all 15A and 20A receptacles in dwelling units must be tamper-resistant since NEC 2008. Applies to all rooms. See §406.12." },
  { q:"How many circuits in a kitchen?",  icon:"🍳",
    a:"Minimum two 20-amp small appliance circuits for kitchen/dining. One dedicated circuit per major appliance (refrigerator, dishwasher, microwave, garbage disposal). See §210.11." },
  { q:"Is a surge protector required?",   icon:"⚡",
    a:"YES — new in NEC 2023. A Type 1 or Type 2 surge protective device (SPD) is now required at all new service entrances. See §230.67." },
  { q:"Do I need an exterior disconnect?", icon:"🚪",
    a:"YES — new in NEC 2023. All new services to one- and two-family homes must have an exterior emergency disconnect marked 'EMERGENCY DISCONNECT' for first responders. See §230.85." },
  { q:"Can I use white wire as a hot?",   icon:"⚪",
    a:"Only if re-identified with black or colored tape at both ends. White is normally neutral. Never use white as hot without re-identification. See §200.6." },
  { q:"How many ground rods do I need?",  icon:"🔩",
    a:"Typically two ground rods minimum, 6 feet apart. One rod is acceptable only if you can verify resistance is 25 ohms or less. See §250.53." },
  { q:"Where do I bond neutral and ground?", icon:"🔗",
    a:"Only at the main service panel. Never bond neutral to ground at a subpanel — keep them separate. See §250.24." },
  { q:"Do ceiling fans need a special box?", icon:"🌀",
    a:"Yes — standard light boxes cannot support ceiling fans. Must use a fan-rated box rated for the fan weight. See §314.27." },
  { q:"How much wire do I leave in a box?", icon:"📏",
    a:"Minimum 6 inches of free conductor, with at least 3 inches extending outside the box. See §300.14." },
  { q:"Can I put a panel in a bathroom?", icon:"🚿",
    a:"No — panels, disconnects, and overcurrent devices cannot be located in bathrooms. See §240.24." },
  { q:"What size wire for a dryer?",      icon:"👕",
    a:"10 AWG for a 30-amp dryer circuit. Modern dryers require a 4-wire connection (2 hots, neutral, ground). See Table 310.12." },
  { q:"What size wire for a range/stove?", icon:"🍽️",
    a:"6 AWG for a 50-amp range circuit. Requires 4-wire connection (2 hots, neutral, ground). See Table 310.12." },
  { q:"How close can a light be to a shower?", icon:"🚿",
    a:"Non-damp rated fixtures must be outside the zone: within 3 feet horizontal and 8 feet vertical of shower/tub. Zone 2 (3-8 feet from water) needs damp-rated. Wet-rated in direct spray zone. See §410.10." },
  { q:"What is the 80% rule?",            icon:"📊",
    a:"Continuous loads (running 3+ hours) cannot exceed 80% of circuit capacity. A 20-amp circuit can only carry 16 amps continuously. A 15-amp circuit = 12 amps max continuous. See §210.23." },
];

// ─── CATEGORY COLORS ─────────────────────────────────────────────────────────
const CAT_COLORS = {
  "General":"#8a9070","Branch Circuits":"#f5a623","Service":"#a855f7",
  "Grounding & Bonding":"#55a878","Wiring Methods":"#5588e0","Equipment":"#55c8e0",
  "Lighting":"#e8d44d","Feeders":"#f5a623","Load Calculations":"#e08a55",
  "Special Equipment":"#e05555","Special Conditions":"#a855f7",
  "Special Occupancies":"#8a9070","Tables & Calculations":"#55c8e0",
};

// ─── COMPONENT ────────────────────────────────────────────────────────────────
export default function NECReference() {
  const [search, setSearch] = useState("");
  const [cat, setCat] = useState("All");
  const [view, setView] = useState("browse"); // browse | quick | new2023
  const [expanded, setExpanded] = useState({});
  const [chatInput, setChatInput] = useState("");
  const [chatHistory, setChatHistory] = useState([]);
  const [chatLoading, setChatLoading] = useState(false);

  const cats = ["All", ...new Set(NEC_RESIDENTIAL.map(r => r.cat))];

  const filtered = NEC_RESIDENTIAL.filter(r => {
    const matchCat = cat === "All" || r.cat === cat;
    const matchNew = view !== "new2023" || r.newIn2023;
    const matchSearch = !search ||
      r.art.toLowerCase().includes(search.toLowerCase()) ||
      r.title.toLowerCase().includes(search.toLowerCase()) ||
      r.plain.toLowerCase().includes(search.toLowerCase()) ||
      r.keyPoints.some(k => k.toLowerCase().includes(search.toLowerCase()));
    return matchCat && matchSearch && matchNew;
  });

  const filteredQuick = QUICK_ANSWERS.filter(q =>
    !search || q.q.toLowerCase().includes(search.toLowerCase()) ||
    q.a.toLowerCase().includes(search.toLowerCase())
  );

  const sendChat = async () => {
    if (!chatInput.trim()) return;
    const msg = { role: "user", content: chatInput };
    const hist = [...chatHistory, msg];
    setChatHistory(hist); setChatInput(""); setChatLoading(true);
    try {
      const r = await fetch("/api/claude", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          model: "claude-sonnet-4-20250514", max_tokens: 600,
          system: `You are an expert residential electrician and NEC 2023 code authority. Answer questions about NEC 2023 residential electrical code clearly and practically. Always cite the specific NEC article number. Explain in plain English first, then give the code reference. Be specific about what's required, what's prohibited, and what's new in 2023. Keep answers concise — 3-5 sentences max unless a longer explanation is truly needed.`,
          messages: hist
        })
      });
      const d = await r.json();
      setChatHistory([...hist, { role: "assistant", content: d.content?.map(b => b.text || "").join("") || "" }]);
    } catch {
      setChatHistory([...hist, { role: "assistant", content: "Unable to respond. Please try again." }]);
    }
    setChatLoading(false);
  };

  const tog = (art) => setExpanded(e => ({ ...e, [art]: !e[art] }));

  const S = { width: "100%", padding: "10px 13px", background: "rgba(255,255,255,0.04)", border: "1px solid rgba(245,166,35,0.28)", borderRadius: 8, color: "#e8e0d0", fontSize: 13, fontFamily: "Georgia,serif", outline: "none", boxSizing: "border-box" };

  return (
    <div>
      {/* Search */}
      <input
        placeholder="Search any topic, article number, or question... e.g. 'GFCI bathroom' or '210.8'"
        value={search} onChange={e => setSearch(e.target.value)}
        style={{ ...S, marginBottom: 12, fontSize: 13 }}
      />

      {/* View Toggle */}
      <div style={{ display: "flex", gap: 7, marginBottom: 14, flexWrap: "wrap" }}>
        {[["browse","📖 All Articles"],["quick","⚡ Quick Answers"],["new2023","🆕 New in 2023"]].map(([v,l]) => (
          <button key={v} onClick={() => setView(v)} style={{
            padding: "7px 14px", borderRadius: 20, cursor: "pointer", fontSize: 11,
            fontFamily: "monospace", letterSpacing: 1, textTransform: "uppercase",
            background: view === v ? "rgba(245,166,35,0.18)" : "rgba(255,255,255,0.03)",
            border: `1px solid ${view === v ? "rgba(245,166,35,0.45)" : "rgba(255,255,255,0.07)"}`,
            color: view === v ? "#f5a623" : "#4a4545", fontWeight: view === v ? 700 : 400
          }}>{l}</button>
        ))}
      </div>

      {/* Category Filter — browse mode */}
      {view === "browse" && (
        <div style={{ display: "flex", flexWrap: "wrap", gap: 5, marginBottom: 14 }}>
          {cats.map(c => (
            <button key={c} onClick={() => setCat(c)} style={{
              padding: "4px 10px", borderRadius: 20, cursor: "pointer", fontSize: 10,
              fontFamily: "monospace",
              background: cat === c ? (CAT_COLORS[c] || "#f5a623") : "rgba(255,255,255,0.025)",
              border: `1px solid ${cat === c ? (CAT_COLORS[c] || "#f5a623") : "rgba(255,255,255,0.07)"}`,
              color: cat === c ? "#0a0a0f" : "#4a4545", fontWeight: cat === c ? 700 : 400
            }}>{c}</button>
          ))}
        </div>
      )}

      <div style={{ fontSize: 10, color: "#2a2030", fontFamily: "monospace", marginBottom: 10 }}>
        {view === "quick" ? `${filteredQuick.length} quick answers` : `${filtered.length} articles`}
      </div>

      {/* ── QUICK ANSWERS VIEW ── */}
      {view === "quick" && (
        <div>
          {filteredQuick.map((qa, i) => (
            <div key={i} style={{ background: "rgba(245,166,35,0.04)", border: "1px solid rgba(245,166,35,0.18)", borderRadius: 10, padding: "14px 16px", marginBottom: 9 }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: "#f5a623", marginBottom: 8 }}>{qa.icon} {qa.q}</div>
              <p style={{ margin: 0, fontSize: 12, color: "#a09888", lineHeight: 1.8 }}>{qa.a}</p>
            </div>
          ))}
        </div>
      )}

      {/* ── NEW IN 2023 VIEW ── */}
      {view === "new2023" && (
        <div>
          <div style={{ background: "rgba(168,85,247,0.08)", border: "1px solid rgba(168,85,247,0.25)", borderRadius: 10, padding: "12px 14px", marginBottom: 16 }}>
            <p style={{ margin: 0, fontSize: 12, color: "#c090f0", lineHeight: 1.7 }}>
              These are the most important changes in NEC 2023 that affect residential work. Know these before your next inspection.
            </p>
          </div>
          {filtered.map(ref => (
            <ArticleCard key={ref.art} ref_={ref} expanded={expanded} tog={tog} CAT_COLORS={CAT_COLORS} />
          ))}
        </div>
      )}

      {/* ── BROWSE VIEW ── */}
      {view === "browse" && filtered.map(ref => (
        <ArticleCard key={ref.art} ref_={ref} expanded={expanded} tog={tog} CAT_COLORS={CAT_COLORS} />
      ))}

      {/* ── AI CODE ASSISTANT ── */}
      <div style={{ marginTop: 24, background: "rgba(245,166,35,0.03)", border: "1px solid rgba(245,166,35,0.18)", borderRadius: 12, padding: 18 }}>
        <div style={{ fontSize: 10, letterSpacing: 3, color: "#f5a623", fontFamily: "monospace", textTransform: "uppercase", marginBottom: 6 }}>💬 Ask the NEC 2023 Code Assistant</div>
        <p style={{ fontSize: 11, color: "#4a4545", margin: "0 0 12px", lineHeight: 1.6 }}>
          Ask any residential code question in plain English. Get a specific answer with the exact NEC article reference.
        </p>
        <div style={{ background: "rgba(0,0,0,0.2)", borderRadius: 8, padding: 12, minHeight: 180, maxHeight: 320, overflowY: "auto", marginBottom: 10 }}>
          {chatHistory.length === 0 && (
            <div style={{ color: "#1a1520", fontSize: 11, fontFamily: "monospace", textAlign: "center", marginTop: 40 }}>
              e.g. "Do I need GFCI in my garage?" or "What's new in NEC 2023 for kitchens?"
            </div>
          )}
          {chatHistory.map((m, i) => (
            <div key={i} style={{ marginBottom: 10, display: "flex", gap: 7, justifyContent: m.role === "user" ? "flex-end" : "flex-start" }}>
              {m.role === "assistant" && (
                <div style={{ width: 20, height: 20, borderRadius: "50%", background: "linear-gradient(135deg,#f5a623,#e8860a)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 10, flexShrink: 0 }}>⚡</div>
              )}
              <div style={{ maxWidth: "83%", background: m.role === "user" ? "rgba(245,166,35,0.09)" : "rgba(255,255,255,0.03)", border: `1px solid ${m.role === "user" ? "rgba(245,166,35,0.2)" : "rgba(255,255,255,0.05)"}`, borderRadius: 8, padding: "8px 12px", fontSize: 12, color: m.role === "user" ? "#d8c060" : "#a09888", lineHeight: 1.7 }}>
                {m.content}
              </div>
            </div>
          ))}
          {chatLoading && <div style={{ color: "#1a1520", fontSize: 11, fontStyle: "italic", fontFamily: "monospace" }}>⚡ Looking up code...</div>}
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <input value={chatInput} onChange={e => setChatInput(e.target.value)}
            onKeyDown={e => e.key === "Enter" && !e.shiftKey && sendChat()}
            placeholder="Ask any NEC 2023 residential question..."
            style={{ ...S, flex: 1, fontSize: 12 }} />
          <button onClick={sendChat} disabled={!chatInput.trim() || chatLoading} style={{
            background: chatInput.trim() ? "linear-gradient(135deg,#f5a623,#e8860a)" : "#0e0e1a",
            border: "none", borderRadius: 8, padding: "0 16px", cursor: chatInput.trim() ? "pointer" : "not-allowed",
            color: chatInput.trim() ? "#0a0a0f" : "#2a2a3a", fontSize: 12, fontFamily: "monospace", fontWeight: 700, flexShrink: 0
          }}>Ask</button>
        </div>
      </div>

      <div style={{ marginTop: 18, padding: 12, background: "rgba(245,166,35,0.03)", border: "1px solid rgba(245,166,35,0.12)", borderRadius: 9 }}>
        <p style={{ margin: 0, fontSize: 10, color: "#3a3838", lineHeight: 1.7 }}>
          ⚠️ These summaries are for quick reference. Always verify with the full NEC 2023 codebook and your local AHJ — local amendments may be stricter. Some jurisdictions have not yet adopted NEC 2023.
        </p>
      </div>
    </div>
  );
}

function ArticleCard({ ref_, expanded, tog, CAT_COLORS }) {
  const color = CAT_COLORS[ref_.cat] || "#f5a623";
  return (
    <div style={{ background: "rgba(255,255,255,0.015)", border: "1px solid rgba(255,255,255,0.05)", borderRadius: 9, marginBottom: 8, overflow: "hidden", borderLeft: `3px solid ${color}` }}>
      <button onClick={() => tog(ref_.art)} style={{ width: "100%", display: "flex", justifyContent: "space-between", alignItems: "flex-start", padding: "12px 14px", background: "none", border: "none", cursor: "pointer", textAlign: "left", gap: 10 }}>
        <div style={{ flex: 1 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap", marginBottom: 4 }}>
            <span style={{ fontSize: 12, fontWeight: 700, color: "#fff", fontFamily: "monospace" }}>§{ref_.art}</span>
            <span style={{ fontSize: 12, color: "#d8d0c0" }}>{ref_.title}</span>
            {ref_.newIn2023 && <span style={{ fontSize: 9, background: "rgba(168,85,247,0.2)", color: "#c090f0", borderRadius: 4, padding: "1px 6px", fontFamily: "monospace", fontWeight: 700 }}>NEW 2023</span>}
            <span style={{ fontSize: 9, background: `${color}18`, color, borderRadius: 4, padding: "1px 6px", fontFamily: "monospace" }}>{ref_.cat}</span>
          </div>
          <p style={{ margin: 0, fontSize: 11, color: "#5a5858", lineHeight: 1.6 }}>{ref_.plain}</p>
        </div>
        <span style={{ color: "#f5a623", fontSize: 12, flexShrink: 0, marginTop: 2 }}>{expanded[ref_.art] ? "▲" : "▼"}</span>
      </button>
      {expanded[ref_.art] && (
        <div style={{ padding: "0 14px 14px", borderTop: "1px solid rgba(255,255,255,0.04)" }}>
          <div style={{ fontSize: 10, letterSpacing: 2, color: "#f5a623", fontFamily: "monospace", textTransform: "uppercase", margin: "12px 0 8px" }}>Key Points</div>
          {ref_.keyPoints.map((pt, i) => (
            <div key={i} style={{ display: "flex", gap: 8, marginBottom: 6 }}>
              <span style={{ color, fontSize: 12, flexShrink: 0, marginTop: 1 }}>→</span>
              <span style={{ fontSize: 12, color: "#888080", lineHeight: 1.6 }}>{pt}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
