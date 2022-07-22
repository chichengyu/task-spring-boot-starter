# task-spring-boot-starter

## 介绍
自定义spring定时任务starter，sql可执行创建表。有两种使用方式： 
  - `spring自带的 TaskScheduler (持久化时执行 sql/job.sql) `  
  - `Quartz 的使用 (持久化时执行 sql/quartz.sql )`

如果要查看任务` debug `日志，需要在项目配置文件` application.yml `配置
```
logging:
  level:
    com.job.task: debug
```
重启项目，可以看到控制台任务` debug `日志已经打印了。

#### 使用说明

使用很简单，项目里引入坐标，`<= 1.2.7.RELEASE `(使用spring自带的 ` TaskScheduler `,没有集成 ` Quartz `)
```
<!-- 使用spring自带的 TaskScheduler -->
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>1.2.7.RELEASE</version>
</dependency>

<!-- 1.2.10.RELEASE开始集成 Quartz -->
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>1.2.10.RELEASE</version>
</dependency>
```
创建任务Bean类  ` TestTask `，需要实现接口 ` com.job.task.ITask<string> `
```
@Component("testTask")
public class TestTask implements ITask<String> {

    @Override
    public R<String> run(String params) {
        System.out.println("测试定时任务执行了，参数:"+params);
        try {
            JobTask jobTask = JSON.parseObject(s, JobTask.class);
            System.out.println(jobTask.toString());
            // 逻辑处理... 
            return R.ok();
        } catch (Exception e) {
            // 会将异常信息记录到 JobTaskLog 的 error 成员中
            return R.error(e.toString());
        }
    }
}
``` 
### 方式一(spring自带的 TaskScheduler)
这引入依赖，可排除多余的 ` Quartz 依赖`,也可以引入之前的 ` 1.2.7.RELEASE `版本
```
<!-- 1.2.10.RELEASE开始集成 Quartz -->
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>1.2.10.RELEASE</version>
    <!-- 方式一可排除 Quartz -->
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
创建一个配置文件 ` TaskConfig.java `
```
@Configuration
public class TaskConfig {

    @Autowired
    private JobLogDao jobLogDao;// 保存到数据库,需自行实现

    @Bean
    public TaskManager taskManager(){
        // 不需要记录日志到数据库
        TaskManager taskManager = new TaskManager();
        taskManager.setPoolSize(5);
        taskManager.setPrefix("task_");
        taskManager.setErrorHandler(e -> {
            log.error("执行异常：{}",e);
            // 可以给管理者发送邮件 ...
        });
        taskManager.init();// 初始化

        // 需要记录日志到数据库
        /*TaskManager taskManager = new TaskManager(jobLog -> jobLogDao.save(jobLog));// 保存定时任务日志到数据库中
        taskManager.setPoolSize(5);
        taskManager.setPrefix("task_");
        taskManager.setErrorHandler(e -> {
            log.error("执行异常：{}",e);
            // 可以给管理者发送邮件 ...
        });
        //taskManager.setJobTaskLogSave(jobLog -> jobLogDao.save(jobLog));//也可以这样设置
        taskManager.init();// 初始化
        return taskManager;*/
    }
}
```
然后创建一个控制器 ` TestController `
```
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private TaskManager taskManager;

    @GetMapping("/task/add")
    public String add() {
        JobTask jobTask = new JobTask();
        jobTask.setBeanName("testTask");// TestTask 类的 BeanName名称
        jobTask.setJobId(100L);
        jobTask.setStatus(0);// 0正常1禁止   JobTaskLog 日志的 status 0代表定时任务运行结果成功 1失败
        jobTask.setParams("我是测试定时器");
        jobTask.setCron("*/2 * * * * ?");
        jobTask.setRemark("测试");
        jobTask.setCreateTime(new Date());
        taskManager.addCronTask(jobTask);
        // taskManager.updateCronTask(jobTask);// 更新任务
        // taskManager.runNow(jobTask);// 立即执行
        // taskManager.cancel(jobTask.getJobId());// 取消任务
        // taskManager.refresh();// 批量添加任务
        // 正在运行的任务
        // ConcurrentHashMap<Long, TaskManager.ScheduledRealTaskFuture> taskContainer = taskManager.getTaskContainer();
        return "ok";
    }
}
```
然后启动项目，访问接口，即可看到定时任务执行。
### 方式二(Quartz方式)
创建一个配置文件 ` TaskQuartzConfig.java `
```
@Slf4j
@Configuration
public class TaskQuartzConfig {

    /**
     * 必须创建一个 SchedulerFactoryBean 加入到spring容器
     * Quartz 的持久化配置也可以在处配置
     * @return
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(){
    //public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource){
        // 默认内存方式 org.quartz.simpl.RAMJobStore
        SchedulerFactoryBean factoryBean = TaskQuartzManager.getSchedulerFactoryBean();
        /* 可选，默认在 TaskQuartzManager.getSchedulerFactoryBean() 中已经进行配置了
        Properties prop = new Properties();
        prop.put("org.quartz.scheduler.instanceName", "TaskScheduler");// 可自定义
        prop.put("org.quartz.scheduler.instanceId", "AUTO");// 可自定义
        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");// 可自定义
        prop.put("org.quartz.threadPool.threadCount", "20");// 可自定义线程
        prop.put("org.quartz.threadPool.threadPriority", "5");// 可自定义
        factoryBean.setQuartzProperties(prop);*/
        factoryBean.setStartupDelay(5);// 项目启动后5s后开始执行任务

        /*// 这是笔者项目中使用的持久化配置
        //SchedulerFactoryBean factory = TaskQuartzManager.getSchedulerFactoryBean();//也可进行覆盖设置
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);
        //quartz参数
        Properties prop = new Properties();
        prop.put("org.quartz.scheduler.instanceName", "TaskScheduler");
        prop.put("org.quartz.scheduler.instanceId", "AUTO");
        //线程池配置
        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        prop.put("org.quartz.threadPool.threadCount", "20");
        prop.put("org.quartz.threadPool.threadPriority", "5");
        //JobStore配置
        //quartz版本不同可能此配置也不同，如启动报错(就是此配置原因)：org.quartz.SchedulerConfigException: DataSource name not set
        //prop.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        prop.put("org.quartz.jobStore.class", "org.springframework.scheduling.quartz.LocalDataSourceJobStore");
        //集群配置
        prop.put("org.quartz.jobStore.isClustered", "false");// 集群时一定要设置为 true
        prop.put("org.quartz.jobStore.clusterCheckinInterval", "15000");
        prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", "1");

        prop.put("org.quartz.jobStore.misfireThreshold", "12000");
        prop.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
        prop.put("org.quartz.jobStore.selectWithLockSQL", "SELECT * FROM {0}LOCKS UPDLOCK WHERE LOCK_NAME = ?");

        //PostgreSQL数据库，需要打开此注释
        //prop.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");

        factory.setQuartzProperties(prop);
        factory.setSchedulerName("TaskScheduler");
        //延时启动
        factory.setStartupDelay(30);
        factory.setApplicationContextSchedulerContextKey("applicationContextKey");
        //可选，QuartzScheduler 启动时更新己存在的Job，这样就不用每次修改targetObject后删除qrtz_job_details表对应记录了
        factory.setOverwriteExistingJobs(true);
        //设置自动启动，默认为true
        factory.setAutoStartup(true);*/

        return factoryBean;
    }

    @Autowired
    private JobLogDao jobLogDao;// 保存到数据库,需自行实现

    /**
     * 创建一个 TaskQuartzManager 用于管理任务
     * @return
     */
    @Bean
    public TaskQuartzManager taskQuartzManager(SchedulerFactoryBean schedulerFactoryBean){
        // 把任务日志保存到数据库
        //TaskQuartzManager taskQuartzManager = new TaskQuartzManager(jobTaskLog -> jobLogDao.save(jobTaskLog));
        TaskQuartzManager taskQuartzManager = new TaskQuartzManager();
        taskQuartzManager.setSchedulerFactoryBean(schedulerFactoryBean);
        taskQuartzManager.setJobTaskLogSave(jobTaskLog -> log.info("日志，[{}]",jobTaskLog));// 此处可以把任务日志保存到数据库
        //taskQuartzManager.setJobNamePrefix("aaaa_");// 可设置任务名称前缀
        taskQuartzManager.init();// 初始化
        return taskQuartzManager;
    }
}
```
然后创建一个控制器 ` TestController `添加任务与上面一样。
```
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private TaskQuartzManager taskQuartzManager;

    @GetMapping("/task/add")
    public String add() {
        JobTask jobTask = new JobTask();
        jobTask.setBeanName("testTask");// TestTask 类的 BeanName名称
        jobTask.setJobId(100L);
        jobTask.setStatus(0);// 0正常1禁止   JobTaskLog 日志的 status 0代表定时任务运行结果成功 1失败
        jobTask.setParams("我是测试定时器");
        jobTask.setCron("*/2 * * * * ?");
        jobTask.setRemark("测试");
        jobTask.setCreateTime(new Date());
        taskQuartzManager.addCronTask(jobTask);
        return "ok";
    }
}
```
启动项目，可以看到定时任务启动了。
