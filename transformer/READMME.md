修改字节码,插入耗时统计代码来分析函数耗时

使用方式:
1.本地先找到transformer的gradle 执行publishToMavenLocal任务
2.添加插件的classpath 依赖       
一般是项目根目录的build.gradle
classpath 'com.chaoxing.transformer:com.chaoxing.transformer.gradle.plugin:1.0'
3.在需要使用的模块的build.gradle添加插件和配置
id 'com.chaoxing.transformer'

methodTimerConfig {
    classesToTransform = [
      'com.example.asmdemo.MainActivity', 'com.example.asmdemo.TestUtils']
    packagesToTransform= [
     'com.chaoxing.mobile.conferencesw'
    ]
}

classesToTransform 配置的是 指定要修改的具体class 比如我就要只修改课堂页面的Activity
packagesToTransform 配置一个包名，可以是多个,对这些包下的所有class 修改

4.需要在app下创建一个目录为 com.chaoxing.transformer 的工具类
MethodTimerLogUtil

这个工具类的作用就是在修改字节码的时候 最后打印日志时要调用的统一入口
在这里面做一些判断 比如对耗时300ms以上的才打印log 

public class MethodTimerLogUtil {

    private static final String TAG = "MethodTimerLogUtil";

    public static final int SLOW_TIME = 50;

    public static void log(String method,long cost){
        if(cost >= SLOW_TIME){
            Log.d(TAG,"source:"+method+" cost:"+cost+"ms "+ "thread:"+Thread.currentThread().getName());
        }
    }
}



