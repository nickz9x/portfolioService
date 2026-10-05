package ChainTracker.portfolioService.model;

import ChainTracker.portfolioService.enums.PositionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    private String name;

    private String assetId;

    private String symbol;

    private String exchange;

    @Column(precision = 38, scale = 18)
    private BigDecimal quantity;

    @Column(precision = 38, scale = 18)
    private BigDecimal averagePrice;

    @Enumerated(EnumType.STRING)
    private PositionType type;

    public void buyAsset(BigDecimal newQuantity,BigDecimal currentPrice){
        BigDecimal currentTotalValue = this.quantity.multiply(this.averagePrice);

        BigDecimal newPurchaseValue = newQuantity.multiply(currentPrice);

        BigDecimal newTotalValue = currentTotalValue.add(newPurchaseValue);

        this.quantity = this.quantity.add(newQuantity);
        this.averagePrice = newTotalValue.divide(this.quantity, 12,RoundingMode.HALF_UP);
    }

    public void sellAsset(BigDecimal quantityToSell) {
        if (quantityToSell.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("A quantidade a vender deve ser maior que zero.");
        }

        if (quantityToSell.compareTo(this.quantity) > 0) {
            throw new IllegalArgumentException("Saldo insuficiente. Você possui apenas: " + this.quantity);
        }

        this.quantity = this.quantity.subtract(quantityToSell);

        if (this.quantity.compareTo(BigDecimal.ZERO) == 0) {
            this.averagePrice = BigDecimal.ZERO;
        }
    }


    public BigDecimal calculePNL(BigDecimal currentPrice){
        if (currentPrice == null || this.averagePrice == null || this.quantity == null){
            return null;
        }
        if (this.quantity.compareTo(BigDecimal.ZERO) == 0){
            return BigDecimal.ZERO;
        }
        if (this.averagePrice.compareTo(BigDecimal.ZERO) == 0){
            return null;
        }
        BigDecimal price = currentPrice.subtract(averagePrice);
        if (price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return price.divide(averagePrice, 12, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal positionValue(BigDecimal currentPrice){
        return currentPrice.multiply(quantity).setScale(12,RoundingMode.HALF_UP);
    }

    public BigDecimal pnlValue(BigDecimal currentPrice) {
        return currentPrice
                .subtract(averagePrice)
                .multiply(quantity);
    }
}
