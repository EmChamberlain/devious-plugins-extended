package net.unethicalite.plugins.buylooper;

public class HalfAndHalfOperation implements LooperOperation
{

    @Override
    public boolean restock(int budget, int primary, int secondary, int tertiary)
    {
        return false;
    }

    @Override
    public boolean moreToOperate(int primary, int secondary, int tertiary)
    {
        return false;
    }

    @Override
    public boolean getInventory(int primary, int secondary, int tertiary)
    {
        return false;
    }

    @Override
    public boolean checkInventory(int primary, int secondary, int tertiary)
    {
        return false;
    }

    @Override
    public boolean operateInventory(int primary, int secondary, int tertiary)
    {
        return false;
    }
}
