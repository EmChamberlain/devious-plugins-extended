package net.unethicalite.plugins.buylooper;

public interface LooperLiquidate
{
    static int ALCH = 1;
    static int GE = 2;

    boolean moreToLiquidate(int primary, int secondary, int tertiary);
    boolean liquidate(int primary, int secondary, int tertiary);
}
