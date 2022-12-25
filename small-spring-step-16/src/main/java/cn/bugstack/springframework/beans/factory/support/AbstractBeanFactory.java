package cn.bugstack.springframework.beans.factory.support;

import cn.bugstack.springframework.beans.BeansException;
import cn.bugstack.springframework.beans.factory.BeanFactory;
import cn.bugstack.springframework.beans.factory.FactoryBean;
import cn.bugstack.springframework.beans.factory.config.BeanDefinition;
import cn.bugstack.springframework.beans.factory.config.BeanPostProcessor;
import cn.bugstack.springframework.beans.factory.config.ConfigurableBeanFactory;
import cn.bugstack.springframework.util.ClassUtils;
import cn.bugstack.springframework.util.StringValueResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * BeanDefinition注册表接口
 * 整合父类的IOC容器以及工厂IOC容器，定义了getBean的方式
 */
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

    /**
     * ClassLoader to resolve bean class names with, if necessary
     */
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    /**
     * BeanPostProcessors to apply in createBean
     */
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();

    /**
     * String resolvers to apply e.g. to annotation attribute values
     */
    private final List<StringValueResolver> embeddedValueResolvers = new ArrayList<>();

    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return doGetBean(name, args);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return (T) getBean(name);
    }

    /**
     * 1. 从三级缓存中取 bean，然后根据是否是 FactoryBean 决定是直接返回还是 getObject后返回
     * 2. 如果三级缓存中没有，则调用 createBean 创建，然后 根据是否是 FactoryBean 决定是直接返回还是 getObject后返回
     */
    protected <T> T doGetBean(final String name, final Object[] args) {
        // 1、从存储单例bean的IOC容器中拿
        Object sharedInstance = getSingleton(name);
        if (sharedInstance != null) {
            // 2、如果是 FactoryBean，则从其他容器拿，这个容器中存储所有factoryBean的getObject （FactoryBean#getObject）
            return (T) getObjectForBeanInstance(sharedInstance, name);
        }

        // 3、IOC容器中没有，则通过beanDefinition创建，然后再根据是否是factoryBean走直接返回还是从单独的容器中取的逻辑
        BeanDefinition beanDefinition = getBeanDefinition(name);
        Object bean = createBean(name, beanDefinition, args);
        return (T) getObjectForBeanInstance(bean, name);
    }

    /**
     * 如果不是 FactoryBean，直接返回beanInstance
     * 如果是 FactoryBean
     * 1、首先尝试从缓存中获取
     * 2、如果缓存中没有，则调用 factory.getObject(),然后塞入缓存中
     */
    private Object getObjectForBeanInstance(Object beanInstance, String beanName) {
        if (!(beanInstance instanceof FactoryBean)) {
            return beanInstance;
        }

        Object object = getCachedObjectForFactoryBean(beanName);

        if (object == null) {
            FactoryBean<?> factoryBean = (FactoryBean<?>) beanInstance;
            object = getObjectFromFactoryBean(factoryBean, beanName);
        }

        return object;
    }

    protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

    protected abstract Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException;

    @Override
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.remove(beanPostProcessor);
        this.beanPostProcessors.add(beanPostProcessor);
    }

    @Override
    public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
        this.embeddedValueResolvers.add(valueResolver);
    }

    /**
     * 调用resolver.resolveStringValue 迭代处理 value
     */
    @Override
    public String resolveEmbeddedValue(String value) {
        String result = value;
        for (StringValueResolver resolver : this.embeddedValueResolvers) {
            result = resolver.resolveStringValue(result);
        }
        return result;
    }

    /**
     * Return the list of BeanPostProcessors that will get applied
     * to beans created with this factory.
     */
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanPostProcessors;
    }

    public ClassLoader getBeanClassLoader() {
        return this.beanClassLoader;
    }

}
