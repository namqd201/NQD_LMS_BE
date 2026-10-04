package com.nqd.nqd_lms_be.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RankTier {
    BRONZE("Đồng", 0, 499),
    SILVER("Bạc", 500, 1499),
    GOLD("Vàng", 1500, 2999),
    PLATINUM("Bạch Kim", 3000, 5999),
    DIAMOND("Kim Cương", 6000, Long.MAX_VALUE);

    private final String displayName;
    private final long minXp;
    private final long maxXp;

    public static RankTier fromTotalXp(long totalXp) {
        if (totalXp >= DIAMOND.minXp) return DIAMOND;
        if (totalXp >= PLATINUM.minXp) return PLATINUM;
        if (totalXp >= GOLD.minXp) return GOLD;
        if (totalXp >= SILVER.minXp) return SILVER;
        return BRONZE;
    }
}
