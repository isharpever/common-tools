package com.isharpever.tool.utils;

import com.google.common.collect.Lists;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.beans.BeanCopier;

public class BeanConverUtil {

	private static Logger logger = LoggerFactory.getLogger(BeanConverUtil.class);

	public static <T, N extends Object> List<T> batchConverObject(List<N> soruceData, Class<N> source,
			Class<T> target) {
		BeanCopier beanCopier = BeanCopier.create(source, target, false);
		List<T> ret = Lists.newArrayList();
		try {
			for (N item : soruceData) {
				T t = target.newInstance();
				beanCopier.copy(item, t, null);
				ret.add(t);
			}
		} catch (Exception e) {
			logger.error("数据转换异常", e);
		}
		return ret;
	}

	public static <T extends Object> T converObject(Object soruceData, Class<T> target) {
		if (soruceData == null)
			return null;
		BeanCopier beanCopier = BeanCopier.create(soruceData.getClass(), target, false);
		try {
			T t = target.newInstance();
			beanCopier.copy(soruceData, t, null);
			return t;
		} catch (Exception e) {
			logger.error("数据转换异常", e);
		}
		return null;
	}
}
