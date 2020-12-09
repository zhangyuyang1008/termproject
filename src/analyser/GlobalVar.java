package analyser;

//全局变量类
public class GlobalVar {
    //是否为常量
    private boolean is_const;
    //字节数
    private int valueCount;
    //按字节顺序排列的变量值
    private String valueItems;

    public GlobalVar(boolean isConst, int valueCount, String valueItems) {
        this.is_const = isConst;
        this.valueCount = valueCount;
        this.valueItems = valueItems;
    }

    public GlobalVar(boolean isConst) {
        this.is_const = isConst;
    }
    public boolean is_const() {
        return is_const;
    }

    public int getValueCount() {
        return valueCount;
    }

    public String getValueItems() {
        return valueItems;
    }
    @Override
    public String toString() {
        return "Global{" +
                "isConst=" + is_const +
                ", valueCount=" + valueCount +
                ", valueItems=" + valueItems +
                '}';
    }
}
