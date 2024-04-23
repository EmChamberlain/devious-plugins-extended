package net.unethicalite.plugins.buylooper;

public interface LooperOperation
{
    int HALF_AND_HALF = 1;
    int USE_TOOL_ON = 2;

    boolean restock(int budget, int primary, int secondary, int tertiary);
    boolean moreToOperate(int primary, int secondary, int tertiary);
    boolean getInventory(int primary, int secondary, int tertiary);
    boolean checkInventory(int primary, int secondary, int tertiary);
    boolean operateInventory(int primary, int secondary, int tertiary);
}
