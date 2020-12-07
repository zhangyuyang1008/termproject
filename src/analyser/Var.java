package analyser;

//变量类
public class Var {
    //变量名
    private String name;
    //编号
    private int id;
    //层
    private int level;

    public Var(String name, Integer id, int level) {
        this.name = name;
        this.id = id;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }
}
