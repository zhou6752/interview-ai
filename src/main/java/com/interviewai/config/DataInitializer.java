package com.interviewai.config;

import com.interviewai.entity.InterviewQuestion;
import com.interviewai.repository.InterviewQuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// DataInitializer —— 数据初始化器
// 实现了 CommandLineRunner 接口，Spring Boot 启动完成后会自动执行 run() 方法
// 作用：第一次启动项目时，自动往数据库里插入 35 道高质量的 Java 面试题
//       这样别人 clone 项目后直接启动就能用，不用手动导数据
@Component  // @Component 让 Spring 管理这个类（不需要自己 new）
public class DataInitializer implements CommandLineRunner {

    private final InterviewQuestionRepository questionRepository;

    // 构造方法注入：Spring 把 InterviewQuestionRepository 自动传进来
    public DataInitializer(InterviewQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    // CommandLineRunner 的唯一方法：项目启动完成后自动执行
    // String... args 是启动时传入的命令行参数（这里用不到）
    @Override
    public void run(String... args) {
        // 幂等性检查：如果表里已经有数据了，就不重复插入
        // 否则每次重启都会插入一遍，导致重复数据
        if (questionRepository.count() > 0) {
            return;  // 已经有题了，跳过初始化
        }

        // 下面全部是 35 道 Java 面试题的初始化数据
        // 覆盖 6 大高频面试分类，每个分类 5-6 道题
        // 难度分布合理：easy 打基础、medium 深入原理、hard 考察底层

        // ========== 集合（6道）==========
        questionRepository.save(new InterviewQuestion("集合", "HashMap 和 Hashtable 的区别是什么？", "HashMap 线程不安全、允许 null 键值、初始容量 16、扩容 2 倍；Hashtable 线程安全（synchronized）、不允许 null、初始容量 11、扩容 2n+1", "easy"));
        questionRepository.save(new InterviewQuestion("集合", "ConcurrentHashMap 怎么保证线程安全的？", "1.7 分段锁（Segment 继承 ReentrantLock），1.8 改用 synchronized + CAS，锁粒度更细", "medium"));
        questionRepository.save(new InterviewQuestion("集合", "HashMap 的 put 流程是怎么样的？", "计算 hash → 找桶 → 空则直接放，不空则链表/红黑树插入 → 超过阈值则扩容", "medium"));
        questionRepository.save(new InterviewQuestion("集合", "HashMap 扩容时为什么是 2 倍？", "保证扩容后元素要么在原位置，要么在原位置+旧容量位置，rehash 时用位运算 & 替代取模，性能更高", "hard"));
        questionRepository.save(new InterviewQuestion("集合", "ArrayList 和 LinkedList 的区别？", "ArrayList 基于数组，内存连续，随机查找快 O(1)，插入删除慢 O(n)；LinkedList 双向链表，随机查找慢 O(n)，头尾插入删除快 O(1)", "easy"));
        questionRepository.save(new InterviewQuestion("集合", "HashSet 底层实现原理？", "基于 HashMap 实现，存到 HashMap 的 key 位置，value 统一存一个 PRESENT 常量对象", "easy"));

        // ========== 多线程（6道）==========
        questionRepository.save(new InterviewQuestion("多线程", "synchronized 和 ReentrantLock 的区别？", "synchronized 是 JVM 关键字，自动加解锁；ReentrantLock 是 API，需手动 lock/unlock，支持公平锁、可中断、条件等待", "medium"));
        questionRepository.save(new InterviewQuestion("多线程", "volatile 关键字的作用？", "保证可见性（写操作立即刷新到主存，读从主存读）、禁止指令重排序，但不保证原子性", "medium"));
        questionRepository.save(new InterviewQuestion("多线程", "线程池的核心参数有哪些？", "corePoolSize（核心线程数）、maximumPoolSize（最大线程数）、keepAliveTime（空闲存活时间）、workQueue（任务队列）、ThreadFactory（线程工厂）、RejectedExecutionHandler（拒绝策略）", "medium"));
        questionRepository.save(new InterviewQuestion("多线程", "线程池的拒绝策略有哪些？", "AbortPolicy（抛异常）、CallerRunsPolicy（调用者线程执行）、DiscardPolicy（静默丢弃）、DiscardOldestPolicy（丢弃最旧任务）", "easy"));
        questionRepository.save(new InterviewQuestion("多线程", "ThreadLocal 的原理是什么？有什么坑？", "每个线程维护一个 ThreadLocalMap，key 是 ThreadLocal 弱引用，value 是值。坑：内存泄漏（key 被回收后 value 还在），用完后需调用 remove()", "hard"));
        questionRepository.save(new InterviewQuestion("多线程", "sleep() 和 wait() 有什么区别？", "sleep 不释放锁，wait 释放锁；sleep 是 Thread 方法，wait 是 Object 方法；sleep 自动醒，wait 需 notify/notifyAll 唤醒", "easy"));

        // ========== JVM（5道）==========
        questionRepository.save(new InterviewQuestion("JVM", "JVM 内存区域有哪些？", "堆（对象实例）、方法区（类信息/常量/静态变量）、虚拟机栈（局部变量表/操作数栈）、本地方法栈、程序计数器", "medium"));
        questionRepository.save(new InterviewQuestion("JVM", "堆内存怎么分代的？为什么要分代？", "新生代（Eden + Survivor 0/1）+ 老年代。分代是为了 GC 效率，大部分对象朝生夕死，新生代频繁 GC 开销小", "medium"));
        questionRepository.save(new InterviewQuestion("JVM", "垃圾回收算法有哪些？", "标记-清除（有碎片）、标记-复制（浪费空间）、标记-整理（无碎片但慢）、分代收集（新生代复制，老年代标记-整理）", "hard"));
        questionRepository.save(new InterviewQuestion("JVM", "什么是类加载的双亲委派模型？", "加载一个类时先委托父类加载器加载，父类加载不了才自己加载。作用：防止核心类库被篡改（如自己写的 java.lang.String 不会被加载）", "hard"));
        questionRepository.save(new InterviewQuestion("JVM", "Minor GC 和 Full GC 的区别？", "Minor GC 清理新生代，频率高速度快；Full GC 清理整个堆和方法区，频率低速度慢，触发条件：老年代满、System.gc() 调用等", "medium"));

        // ========== Spring（6道）==========
        questionRepository.save(new InterviewQuestion("Spring", "Spring IoC 是什么？解决了什么问题？", "IoC（控制反转）将对象创建和依赖管理的权力交给 Spring 容器。解决了对象间的耦合问题，由容器管理对象的生命周期和依赖关系", "easy"));
        questionRepository.save(new InterviewQuestion("Spring", "Spring AOP 是什么？应用场景有哪些？", "AOP（面向切面编程）通过动态代理在运行时增强方法。应用：日志、事务管理、权限校验、性能监控", "medium"));
        questionRepository.save(new InterviewQuestion("Spring", "Spring Boot 自动配置原理？", "@EnableAutoConfiguration → 加载 META-INF/spring.factories → 按条件注解（@Conditional）判断是否生效", "hard"));
        questionRepository.save(new InterviewQuestion("Spring", "@Autowired 和 @Resource 的区别？", "@Autowired 是 Spring 的，按类型注入；@Resource 是 Java 的，默认按名称注入，找不到再按类型", "medium"));
        questionRepository.save(new InterviewQuestion("Spring", "Spring 事务的传播行为有哪些？", "REQUIRED（有则加入无则新建）、REQUIRES_NEW（挂起当前新建）、SUPPORTS、NOT_SUPPORTED、MANDATORY、NEVER、NESTED", "hard"));
        questionRepository.save(new InterviewQuestion("Spring", "Bean 的生命周期？", "实例化 → 属性赋值 → 各种 Aware 回调 → BeanPostProcessor 前置处理 → init-method → BeanPostProcessor 后置处理 → 使用 → destroy", "medium"));

        // ========== MySQL（5道）==========
        questionRepository.save(new InterviewQuestion("MySQL", "什么是索引？有什么用？", "索引是帮助 MySQL 高效获取数据的数据结构。类似字典目录，避免全表扫描，提高查询速度", "easy"));
        questionRepository.save(new InterviewQuestion("MySQL", "InnoDB 和 MyISAM 的区别？", "InnoDB 支持事务/行锁/外键/崩溃恢复，MyISAM 只支持表锁/不支持事务/支持全文索引更快", "medium"));
        questionRepository.save(new InterviewQuestion("MySQL", "B+ 树索引和 Hash 索引的区别？", "B+ 树支持范围查询和排序，Hash 只支持等值查询；B+ 树有顺序性，Hash 无序；Hash 查询单条更快", "hard"));
        questionRepository.save(new InterviewQuestion("MySQL", "事务的 ACID 特性是什么？", "原子性（要么全做要么全不做）、一致性（数据状态一致）、隔离性（事务互不干扰）、持久性（提交后永久保存）", "easy"));
        questionRepository.save(new InterviewQuestion("MySQL", "MySQL 的隔离级别有哪些？", "读未提交（脏读）、读已提交（不可重复读）、可重复读（MySQL 默认，幻读）、串行化", "medium"));

        // ========== Redis（6道）==========
        questionRepository.save(new InterviewQuestion("Redis", "Redis 有哪些数据结构？", "String、List、Set、ZSet（有序集合）、Hash，还有 Bitmap、HyperLogLog、GEO", "easy"));
        questionRepository.save(new InterviewQuestion("Redis", "Redis 的过期删除策略？", "定期删除（每隔 100ms 随机检查一批过期 key）+ 惰性删除（访问时检查是否过期），内存不足时触发淘汰策略", "medium"));
        questionRepository.save(new InterviewQuestion("Redis", "Redis 的持久化方式有哪些？", "RDB（快照，全量保存，恢复快丢失多）+ AOF（追加日志，丢失少恢复慢），可混合使用", "medium"));
        questionRepository.save(new InterviewQuestion("Redis", "什么是缓存穿透、缓存击穿、缓存雪崩？怎么解决？", "穿透（查不存在的数据）：布隆过滤器/空值缓存。击穿（热点 key 过期）：互斥锁/逻辑过期。雪崩（大量 key 同时过期）：随机过期时间/多级缓存", "hard"));
        questionRepository.save(new InterviewQuestion("Redis", "Redis 集群模式有哪些？", "主从复制（读写分离）、Sentinel（高可用）、Cluster（分布式分片）", "medium"));
    }
}
