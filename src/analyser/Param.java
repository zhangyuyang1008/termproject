package analyser;

//参数类
public class Param {
    //类型
    private String type;
    //参数名
    private String name;
    //构造方法
    public Param(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
