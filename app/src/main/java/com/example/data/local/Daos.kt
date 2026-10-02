package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio_holdings ORDER BY quantity * avgBuyPrice DESC")
    fun getAllHoldings(): Flow<List<PortfolioHoldingEntity>>

    @Query("SELECT * FROM portfolio_holdings WHERE symbol = :symbol LIMIT 1")
    suspend fun getHoldingBySymbol(symbol: String): PortfolioHoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHolding(holding: PortfolioHoldingEntity)

    @Query("DELETE FROM portfolio_holdings WHERE symbol = :symbol")
    suspend fun deleteHolding(symbol: String)

    @Query("DELETE FROM portfolio_holdings WHERE quantity <= 0.00000001")
    suspend fun clearZeroHoldings()
}

@Dao
interface TradeDao {
    @Query("SELECT * FROM trade_transactions ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<TradeTransactionEntity>>

    @Query("SELECT * FROM trade_transactions WHERE taxYear = :year ORDER BY timestamp ASC")
    fun getTradesForYear(year: Int): Flow<List<TradeTransactionEntity>>

    @Query("SELECT * FROM trade_transactions WHERE taxYear = :year ORDER BY timestamp ASC")
    suspend fun getTradesForYearDirect(year: Int): List<TradeTransactionEntity>

    @Query("SELECT * FROM trade_transactions WHERE symbol = :symbol ORDER BY timestamp ASC")
    suspend fun getTradesForSymbol(symbol: String): List<TradeTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeTransactionEntity): Long
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY id DESC")
    fun getAllAlerts(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE isEnabled = 1 AND isTriggered = 0")
    suspend fun getActiveAlertsDirect(): List<PriceAlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlertEntity): Long

    @Update
    suspend fun updateAlert(alert: PriceAlertEntity)

    @Query("UPDATE price_alerts SET isTriggered = 1, triggeredAt = :timestamp WHERE id = :id")
    suspend fun markAlertTriggered(id: Long, timestamp: Long)

    @Query("UPDATE price_alerts SET isEnabled = :enabled WHERE id = :id")
    suspend fun setAlertEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun deleteAlert(id: Long)
}

@Dao
interface MutualFundDao {
    @Query("SELECT * FROM mutual_fund_holdings ORDER BY totalInvested DESC")
    fun getAllMutualFundHoldings(): Flow<List<MutualFundHoldingEntity>>

    @Query("SELECT * FROM mutual_fund_holdings WHERE fundCode = :code LIMIT 1")
    suspend fun getFundHoldingByCode(code: String): MutualFundHoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFundHolding(holding: MutualFundHoldingEntity)

    @Query("DELETE FROM mutual_fund_holdings WHERE fundCode = :code")
    suspend fun deleteFundHolding(code: String)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet_account WHERE id = 1 LIMIT 1")
    fun getWallet(): Flow<WalletAccountEntity?>

    @Query("SELECT * FROM wallet_account WHERE id = 1 LIMIT 1")
    suspend fun getWalletDirect(): WalletAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWallet(wallet: WalletAccountEntity)

    @Query("UPDATE wallet_account SET cashBalance = :newBalance WHERE id = 1")
    suspend fun updateCashBalance(newBalance: Double)
}
