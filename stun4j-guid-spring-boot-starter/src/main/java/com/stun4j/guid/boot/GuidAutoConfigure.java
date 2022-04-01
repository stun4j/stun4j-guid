/*
 * Copyright 2022 the original author or authors.
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
package com.stun4j.guid.boot;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.stun4j.guid.boot.GuidProperties.Strategy;
import com.stun4j.guid.core.LocalGuid;

/**
 * Responsible for initializing Guid from its boot configuration files.
 * @author Jay Meng
 */
@Configuration
@EnableConfigurationProperties(GuidProperties.class)
public class GuidAutoConfigure implements BeanClassLoaderAware, ApplicationContextAware {
  private static final Logger LOG = LoggerFactory.getLogger(GuidAutoConfigure.class);

  @Bean
  LocalGuid localGuid() {
    return LocalGuid.instance();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    // TODO Auto-generated method stub

  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    // TODO Auto-generated method stub

  }

  GuidAutoConfigure(GuidProperties props, ApplicationArguments appArgs) {
    // guid initialization if necessary
    if (!LocalGuid.isAllowInitialization()) {
      LOG.warn("The local-guid has already been initialized, the initialization initiated by Stf was ignored");
    } else {
      // GuidProperties guidCfg = props.getGuid();// TODO mj:-Dspring. 这种怎么处理
      List<String> guidStrategies;
      if ((guidStrategies = appArgs.getOptionValues("stun4j.guid.strategy")) != null
          && StringUtils.hasText(guidStrategies.get(0))) {
        props.setStrategy(Strategy.valueOf(guidStrategies.get(0).toUpperCase()));
      }

      switch (props.getStrategy()) {
        case ZK:
          appArgs.getOptionValues("stun4j.guid.zk-conn-addr");
          appArgs.getOptionValues("stun4j.guid.zk-namespace");
          // LocalZkGuid.init(null, null);
          break;
        case LOCAL_IP:
          List<String> ipPres;
          if ((ipPres = appArgs.getOptionValues("stun4j.guid.ip-pre")) != null && StringUtils.hasText(ipPres.get(0))) {
            // TODO mj:check ip pattern(very strict)
            LocalGuid.initWithLocalIp(ipPres.get(0));
          } else if (StringUtils.hasText(props.getIpStartWith())) {
            LocalGuid.initWithLocalIp(props.getIpStartWith());
          } else {
            LocalGuid.initWithLocalIp();
            LOG.warn(
                "Automatic selection of local IP is too arbitrary > It is highly recommended that you initialize the local-guid by specifying an IP prefix, or the global uniqueness of the guid could be broken");
          }
          break;
        case MANUAL:
          List<String> dcIds = appArgs.getOptionValues("stun4j.guid.dc-id");
          List<String> wKIds = appArgs.getOptionValues("stun4j.guid.wk-id");
          int dcId, wkId;
          String warnMsg = "You are running the risk of breaking the global uniqueness of local-guid(Cause you choose the 'manual' strategy) > Consider choose other initialization strategy for local-guid";
          if (dcIds != null || wKIds != null) {
            dcId = (dcIds == null || "".equals(dcIds.get(0))) ? 0 : Integer.parseInt(dcIds.get(0));
            wkId = (wKIds == null || "".equals(wKIds.get(0))) ? 0 : Integer.parseInt(wKIds.get(0));
            LocalGuid.init(dcId, wkId);
            LOG.warn(warnMsg);
          } else if ((dcId = props.getDatacenterId()) >= 0 && (wkId = props.getWorkerId()) >= 0) {
            // TODO mj:test yml placeholder
            LocalGuid.init(dcId, wkId);
            LOG.warn(warnMsg);
          } else {
            LocalGuid.init(0, 0);
            LOG.warn(
                "You are running the risk of breaking the global uniqueness of local-guid(Each node takes 0,0 as its dcId and wkId) > It is highly recommended that you choose other initialization strategy for local-guid");
          }
          break;
        default:
          break;
      }
    }
  }

}
