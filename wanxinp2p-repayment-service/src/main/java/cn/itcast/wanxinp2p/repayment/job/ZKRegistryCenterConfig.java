package cn.itcast.wanxinp2p.repayment.job;

import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ZK 注册中心配置类
 * @author old money
 * @create 2022-03-07 15:31
 */
@Configuration
public class ZKRegistryCenterConfig {

    //zookeeper服务地址
    @Value("${p2p.zookeeper.connString}")
    private String ZOOKEEPER_CONNECTION_STRING;

    //定时任务名称空间
    @Value("${p2p.job.namespace}")
    private String JOB_NAMESPACE;

    //创建注册中心
    @Bean(initMethod = "init")
    public ZookeeperRegistryCenter setUpRegistryCenter(){
        //zk的配置
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration(ZOOKEEPER_CONNECTION_STRING, JOB_NAMESPACE);

        //创建注册中心
        ZookeeperRegistryCenter zookeeperRegistryCenter = new ZookeeperRegistryCenter(zookeeperConfiguration);
        return zookeeperRegistryCenter;
    }

}
