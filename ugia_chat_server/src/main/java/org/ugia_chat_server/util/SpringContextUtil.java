package org.ugia_chat_server.util;


import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SpringContextUtil {
	private SpringContextUtil() {}
	private static ApplicationContext applicationContext;
	static {
		applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
	}
	@SuppressWarnings("unchecked")
	public static <T> T getBean(String beanId) {
		T bean = null;
		try {
			if(StringUtils.isNotEmpty(StringUtils.trim(beanId))) {
				bean = (T) applicationContext.getBean(beanId);
			}
		} catch(NoSuchBeanDefinitionException e) {
			e.printStackTrace();
		}
		return bean;
	}
	public static <T> T getBean(String... partName) {
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < partName.length; i++) {
			sb.append(partName[i]);
			if(i != partName.length - 1) {
				sb.append(".");
			}
		}
		return getBean(sb.toString());
	}
}
