/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gateway.config.conditional;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import static org.springframework.boot.autoconfigure.condition.ConditionMessage.forCondition;

/**
 * 因为组件对应的接口不同，用泛型来做通用转换。
 * * @param <T>
 */
public abstract class OnEnabledComponent<T> extends SpringBootCondition
		implements ConfigurationCondition {

	private static final String PREFIX = "spring.cloud.gateway.";

	private static final String SUFFIX = ".enabled";

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

	/**
	 * 这个方法在 SpringBootCondition 接口的 matches 有调用。
	 * annotationClass() 就是子类实现的注解的名字 比如：{@link ConditionalOnEnabledGlobalFilter}
	 * getEndpointType 方法拿到的是 org.springframework.cloud.gateway.filter.LoadBalancerClientFilter
	 * @param context
	 * @param metadata 被注解的方法信息数据，比如方法名字，类名字
	 * @return
	 */
	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {

		Class<? extends T> candidate = getEndpointType(annotationClass(), context,
				metadata);
		return determineOutcome(candidate, context.getEnvironment());
	}

	/**
	 * 获取 被注释方法 返回的类型信息。
	 * @param annotationClass
	 * @param context
	 * @param metadata
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected Class<? extends T> getEndpointType(Class<?> annotationClass,
			ConditionContext context, AnnotatedTypeMetadata metadata) {
		//代码中没有为注解设置 value 所以这块逻辑不会走
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(annotationClass.getName());
		if (attributes != null && attributes.containsKey("value")) {
			Class<?> target = (Class<?>) attributes.get("value");
			if (target != defaultValueClass()) {
				return (Class<? extends T>) target;
			}
		}
		Assert.state(
				metadata instanceof MethodMetadata
						&& metadata.isAnnotated(Bean.class.getName()),
				getClass().getSimpleName()
						+ " must be used on @Bean methods when the value is not specified");
		MethodMetadata methodMetadata = (MethodMetadata) metadata;
		try {
			return (Class<? extends T>) ClassUtils.forName(
					methodMetadata.getReturnTypeName(), context.getClassLoader());
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to extract endpoint id for "
					+ methodMetadata.getDeclaringClassName() + "."
					+ methodMetadata.getMethodName(), ex);
		}
	}

	/**
	 * 该方法主要作用有2点
	 * 1：得到配置的key然后 检查是否设置为false 默认是开启的。
	 * 2：根据配置结果组装condition断言信息对象。
	 * //annotationClass() 就是子类实现的注解的名字 比如：ConditionalOnEnabledGlobalFilter
	 *
	 * @param componentClass
	 * @param resolver
	 * @return
	 */
	private ConditionOutcome determineOutcome(Class<? extends T> componentClass,
			PropertyResolver resolver) {
		String key = PREFIX + normalizeComponentName(componentClass) + SUFFIX;
        //组装Condition断言结果信息。
		ConditionMessage.Builder messageBuilder = forCondition(
				annotationClass().getName(), componentClass.getName());
		//查看配置文件的配置项
		if ("false".equalsIgnoreCase(resolver.getProperty(key))) {
			return ConditionOutcome
					.noMatch(messageBuilder.because("bean is not available"));
		}
		return ConditionOutcome.match();
	}
    //获取组件的名字
	protected abstract String normalizeComponentName(Class<? extends T> componentClass);
    //这个是为了动态获取子类所对应的注解，这样就可以做到 注解关联 condition类，然后类中回调
	protected abstract Class<?> annotationClass();

	protected abstract Class<? extends T> defaultValueClass();

}
