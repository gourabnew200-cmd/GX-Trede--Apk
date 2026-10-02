package com.example.data.model

enum class TaxAccountingMethod(val title: String, val description: String) {
    FIFO("FIFO", "First In, First Out (IRS standard default)"),
    LIFO("LIFO", "Last In, First Out (Best in rising markets)"),
    HIFO("HIFO", "Highest In, First Out (Minimizes current tax liability)")
}

data class TaxableEvent(
    val id: Long,
    val assetSymbol: String,
    val quantity: Double,
    val dateAcquired: String,
    val dateSold: String,
    val proceedsUsd: Double,
    val costBasisUsd: Double,
    val gainLossUsd: Double,
    val isLongTerm: Boolean
)

data class TaxReportSummary(
    val taxYear: Int,
    val accountingMethod: TaxAccountingMethod,
    val totalProceeds: Double,
    val totalCostBasis: Double,
    val netCapitalGain: Double,
    val shortTermGains: Double,
    val shortTermLosses: Double,
    val netShortTerm: Double,
    val longTermGains: Double,
    val longTermLosses: Double,
    val netLongTerm: Double,
    val estimatedTaxLiability: Double,
    val taxableEventsCount: Int,
    val taxableEvents: List<TaxableEvent>
)
