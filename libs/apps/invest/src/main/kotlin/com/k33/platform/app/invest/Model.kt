package com.k33.platform.app.invest

import kotlinx.serialization.Serializable
import java.util.Locale

enum class InvestorType(
    val label: String,
) {
    PROFESSIONAL("Professional"),
    ELECTIVE_PROFESSIONAL("Elective professional"),
    NON_PROFESSIONAL("Unqualified"),
}

@Serializable
data class FundInfoRequest(
    val investorType: InvestorType,
    val name: String? = null,
    val company: String? = null,
    val phoneNumber: PhoneNumber? = null,
    val countryCode: ISO3CountyCode? = null, // ISO 3166 3 character alpha-3 code
    val fundName: String? = null,
)

@Serializable
data class PhoneNumber(
    val countryCode: String,
    val nationalNumber: String,
) {
    override fun toString(): String = "+$countryCode $nationalNumber"
}

enum class Status {
    NOT_REGISTERED,
    NOT_AUTHORIZED,
    REGISTERED,
}

enum class ISO3CountyCode(
    val displayName: String,
) {
    AND("Andorra"),
    ARE("United Arab Emirates"),
    AFG("Afghanistan"),
    ATG("Antigua & Barbuda"),
    AIA("Anguilla"),
    ALB("Albania"),
    ARM("Armenia"),
    AGO("Angola"),
    ATA("Antarctica"),
    ARG("Argentina"),
    ASM("American Samoa"),
    AUT("Austria"),
    AUS("Australia"),
    ABW("Aruba"),
    ALA("Åland Islands"),
    AZE("Azerbaijan"),
    BIH("Bosnia & Herzegovina"),
    BRB("Barbados"),
    BGD("Bangladesh"),
    BEL("Belgium"),
    BFA("Burkina Faso"),
    BGR("Bulgaria"),
    BHR("Bahrain"),
    BDI("Burundi"),
    BEN("Benin"),
    BLM("St. Barthélemy"),
    BMU("Bermuda"),
    BRN("Brunei"),
    BOL("Bolivia"),
    BES("Caribbean Netherlands"),
    BRA("Brazil"),
    BHS("Bahamas"),
    BTN("Bhutan"),
    BVT("Bouvet Island"),
    BWA("Botswana"),
    BLR("Belarus"),
    BLZ("Belize"),
    CAN("Canada"),
    CCK("Cocos (Keeling) Islands"),
    COD("Congo - Kinshasa"),
    CAF("Central African Republic"),
    COG("Congo - Brazzaville"),
    CHE("Switzerland"),
    CIV("Côte d’Ivoire"),
    COK("Cook Islands"),
    CHL("Chile"),
    CMR("Cameroon"),
    CHN("China"),
    COL("Colombia"),
    CRI("Costa Rica"),
    CUB("Cuba"),
    CPV("Cape Verde"),
    CUW("Curaçao"),
    CXR("Christmas Island"),
    CYP("Cyprus"),
    CZE("Czechia"),
    DEU("Germany"),
    DJI("Djibouti"),
    DNK("Denmark"),
    DMA("Dominica"),
    DOM("Dominican Republic"),
    DZA("Algeria"),
    ECU("Ecuador"),
    EST("Estonia"),
    EGY("Egypt"),
    ESH("Western Sahara"),
    ERI("Eritrea"),
    ESP("Spain"),
    ETH("Ethiopia"),
    FIN("Finland"),
    FJI("Fiji"),
    FLK("Falkland Islands"),
    FSM("Micronesia"),
    FRO("Faroe Islands"),
    FRA("France"),
    GAB("Gabon"),
    GBR("United Kingdom"),
    GRD("Grenada"),
    GEO("Georgia"),
    GUF("French Guiana"),
    GGY("Guernsey"),
    GHA("Ghana"),
    GIB("Gibraltar"),
    GRL("Greenland"),
    GMB("Gambia"),
    GIN("Guinea"),
    GLP("Guadeloupe"),
    GNQ("Equatorial Guinea"),
    GRC("Greece"),
    SGS("South Georgia & South Sandwich Islands"),
    GTM("Guatemala"),
    GUM("Guam"),
    GNB("Guinea-Bissau"),
    GUY("Guyana"),
    HKG("Hong Kong SAR China"),
    HMD("Heard & McDonald Islands"),
    HND("Honduras"),
    HRV("Croatia"),
    HTI("Haiti"),
    HUN("Hungary"),
    IDN("Indonesia"),
    IRL("Ireland"),
    ISR("Israel"),
    IMN("Isle of Man"),
    IND("India"),
    IOT("British Indian Ocean Territory"),
    IRQ("Iraq"),
    IRN("Iran"),
    ISL("Iceland"),
    ITA("Italy"),
    JEY("Jersey"),
    JAM("Jamaica"),
    JOR("Jordan"),
    JPN("Japan"),
    KEN("Kenya"),
    KGZ("Kyrgyzstan"),
    KHM("Cambodia"),
    KIR("Kiribati"),
    COM("Comoros"),
    KNA("St. Kitts & Nevis"),
    PRK("North Korea"),
    KOR("South Korea"),
    KWT("Kuwait"),
    CYM("Cayman Islands"),
    KAZ("Kazakhstan"),
    LAO("Laos"),
    LBN("Lebanon"),
    LCA("St. Lucia"),
    LIE("Liechtenstein"),
    LKA("Sri Lanka"),
    LBR("Liberia"),
    LSO("Lesotho"),
    LTU("Lithuania"),
    LUX("Luxembourg"),
    LVA("Latvia"),
    LBY("Libya"),
    MAR("Morocco"),
    MCO("Monaco"),
    MDA("Moldova"),
    MNE("Montenegro"),
    MAF("St. Martin"),
    MDG("Madagascar"),
    MHL("Marshall Islands"),
    MKD("North Macedonia"),
    MLI("Mali"),
    MMR("Myanmar (Burma)"),
    MNG("Mongolia"),
    MAC("Macao SAR China"),
    MNP("Northern Mariana Islands"),
    MTQ("Martinique"),
    MRT("Mauritania"),
    MSR("Montserrat"),
    MLT("Malta"),
    MUS("Mauritius"),
    MDV("Maldives"),
    MWI("Malawi"),
    MEX("Mexico"),
    MYS("Malaysia"),
    MOZ("Mozambique"),
    NAM("Namibia"),
    NCL("New Caledonia"),
    NER("Niger"),
    NFK("Norfolk Island"),
    NGA("Nigeria"),
    NIC("Nicaragua"),
    NLD("Netherlands"),
    NOR("Norway"),
    NPL("Nepal"),
    NRU("Nauru"),
    NIU("Niue"),
    NZL("New Zealand"),
    OMN("Oman"),
    PAN("Panama"),
    PER("Peru"),
    PYF("French Polynesia"),
    PNG("Papua New Guinea"),
    PHL("Philippines"),
    PAK("Pakistan"),
    POL("Poland"),
    SPM("St. Pierre & Miquelon"),
    PCN("Pitcairn Islands"),
    PRI("Puerto Rico"),
    PSE("Palestinian Territories"),
    PRT("Portugal"),
    PLW("Palau"),
    PRY("Paraguay"),
    QAT("Qatar"),
    REU("Réunion"),
    ROU("Romania"),
    SRB("Serbia"),
    RUS("Russia"),
    RWA("Rwanda"),
    SAU("Saudi Arabia"),
    SLB("Solomon Islands"),
    SYC("Seychelles"),
    SDN("Sudan"),
    SWE("Sweden"),
    SGP("Singapore"),
    SHN("St. Helena"),
    SVN("Slovenia"),
    SJM("Svalbard & Jan Mayen"),
    SVK("Slovakia"),
    SLE("Sierra Leone"),
    SMR("San Marino"),
    SEN("Senegal"),
    SOM("Somalia"),
    SUR("Suriname"),
    SSD("South Sudan"),
    STP("São Tomé & Príncipe"),
    SLV("El Salvador"),
    SXM("Sint Maarten"),
    SYR("Syria"),
    SWZ("Eswatini"),
    TCA("Turks & Caicos Islands"),
    TCD("Chad"),
    ATF("French Southern Territories"),
    TGO("Togo"),
    THA("Thailand"),
    TJK("Tajikistan"),
    TKL("Tokelau"),
    TLS("Timor-Leste"),
    TKM("Turkmenistan"),
    TUN("Tunisia"),
    TON("Tonga"),
    TUR("Turkey"),
    TTO("Trinidad & Tobago"),
    TUV("Tuvalu"),
    TWN("Taiwan"),
    TZA("Tanzania"),
    UKR("Ukraine"),
    UGA("Uganda"),
    UMI("US Outlying Islands"),
    USA("United States"),
    URY("Uruguay"),
    UZB("Uzbekistan"),
    VAT("Vatican City"),
    VCT("St. Vincent & Grenadines"),
    VEN("Venezuela"),
    VGB("British Virgin Islands"),
    VIR("US Virgin Islands"),
    VNM("Vietnam"),
    VUT("Vanuatu"),
    WLF("Wallis & Futuna"),
    WSM("Samoa"),
    YEM("Yemen"),
    MYT("Mayotte"),
    ZAF("South Africa"),
    ZMB("Zambia"),
    ZWE("Zimbabwe"),
}

fun main() {
    Locale.getISOCountries().forEach { iso2CountryCode ->
        val locale = Locale.of("", iso2CountryCode)
        println("""${locale.isO3Country}("${locale.displayName}"),""")
    }
}