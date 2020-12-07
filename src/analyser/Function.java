package analyser;

import java.util.List;

//函数类
public class Function {
    //返回值类型
    private String return_type;
    //函数名
    private String function_name;
    //参数声明
    private List<Param> param_list;
    //函数id
    private int id;

    public Function(String type, String name, List<Param> params, Integer id) {
        this.return_type = type;
        this.function_name = name;
        this.param_list = params;
        this.id = id;
    }

    public String getReturn_type() {
        return return_type;
    }

    public String getFunction_name() {
        return function_name;
    }

    public List<Param> getParam_list() {
        return param_list;
    }

    public int getId() {
        return id;
    }

}
