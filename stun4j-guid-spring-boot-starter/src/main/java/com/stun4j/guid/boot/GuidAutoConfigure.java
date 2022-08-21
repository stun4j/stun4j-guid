/*
 * Copyright 2022-? the original author or authors.
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

import static com.stun4j.guid.core.utils.Asserts.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.stun4j.guid.boot.support.CuratorVersion;
import com.stun4j.guid.core.LocalGuid;
import com.stun4j.guid.core.LocalGuidMultiton;
import com.stun4j.guid.core.LocalZkGuid;
import com.stun4j.guid.core.ZkGuidNode;
import com.stun4j.guid.core.utils.Exceptions;
import com.stun4j.guid.core.utils.NetworkUtils;

/**
 * Responsible for initializing Guid from its boot configuration files.
 * @author Jay Meng
 */
@Configuration
@EnableConfigurationProperties(GuidProperties.class)
public class GuidAutoConfigure {
  private static final Logger LOG = LoggerFactory.getLogger(GuidAutoConfigure.class);

  @Bean
  LocalGuid localGuid() {
    return LocalGuid.instance();
  }

  GuidAutoConfigure(GuidProperties props) {
    // global initialization
    Multiton multi;
    if ((multi = props.getMultiton()).isEnabled()) {
      LocalGuidMultiton._enabled = true;
      if (multi.isAutoRegisterEnabled()) {
        LocalGuidMultiton._auto_register_enabled = true;
      } else {
        LocalGuidMultiton._auto_register_enabled = false;
      }
    } else {
      LocalGuidMultiton._enabled = false;
    }

    // instance initialization
    if (!LocalGuid.isAllowInitialization()) {
      LOG.warn("The local-guid has already been initialized, new initialization is ignored!");
      return;
    }

    BitEditing bitEdit = props.getBitEditing();
    switch (props.getStrategy()) {
      case ZK:
        String curatorVer = CuratorVersion.getVersion();
        String advice = "Please check 'https://github.com/stun4j/stun4j-guid/blob/master/README.md#notes' for detail.";
        state(curatorVer != null,
            "You must place 'org.apache.curator:curator-recipes:2.13.0 or 3.3.0+' in your project's classpath,or you can't communicate with zk. "
                + advice);
        LOG.info("Your 'curator-recipes' version is {},the recommended version is 2.13.0 or 3.3.0+. {}", curatorVer,
            advice);
        String ipPreToCheck = null;
        if (StringUtils.hasText(props.getIpStartWith())) {
          ipPreToCheck = props.getIpStartWith();
        }
        String localIp = NetworkUtils.getLocalhost(ipPreToCheck);
        String connStr = localIp + ":2181", nameSpace = ZkGuidNode.DFT_ZK_NAMESPACE_GUID;
        if (StringUtils.hasText(props.getZkConnAddr())) {
          connStr = props.getZkConnAddr();
        }
        if (StringUtils.hasText(props.getZkNamespace())) {
          nameSpace = props.getZkNamespace();
        }
        try {
          if (!bitEdit.isEnabled()) {
            LocalZkGuid.init(connStr, nameSpace, ipPreToCheck);
          } else {
            LocalZkGuid.init(connStr, nameSpace, bitEdit.getDigits(), bitEdit.getDatacenterIdBits(),
                bitEdit.getWorkerIdBits(), bitEdit.getSeqBits(), bitEdit.isFixedDigitsEnabled(), ipPreToCheck);
          }
        } catch (Throwable e) {
          Exceptions.sneakyThrow(e);
        }
        break;
      case LOCAL_IP:
        String ipPreToPick;
        if (StringUtils.hasText(ipPreToPick = props.getIpStartWith())) {
          if (!bitEdit.isEnabled()) {
            LocalGuid.initWithLocalIp(ipPreToPick);// TODO mj:check ip pattern(very strict)
          } else {
            LocalGuid.initWithLocalIp(bitEdit.getDigits(), bitEdit.isShortDcWkIdBitsWhenUsingLocalIpStrategy(),
                bitEdit.getSeqBits(), bitEdit.isFixedDigitsEnabled(), ipPreToPick);
          }
        } else {
          if (!bitEdit.isEnabled()) {
            LocalGuid.initWithLocalIp();
          } else {
            LocalGuid.initWithLocalIp(bitEdit.getDigits(), bitEdit.isShortDcWkIdBitsWhenUsingLocalIpStrategy(),
                bitEdit.getSeqBits(), bitEdit.isFixedDigitsEnabled());
          }
          LOG.warn(
              "Automatic selection of local IP is too arbitrary > Consider initialize the local-guid by specifying an IP prefix, or the global uniqueness of the guid could be broken");
        }
        break;
      case MANUAL:
        int dcId = props.getDatacenterId(), wkId = props.getWorkerId();
        if (!bitEdit.isEnabled()) {
          LocalGuid.init(dcId, wkId);
        } else {
          LocalGuid.init(dcId, dcId, bitEdit.getDigits(), bitEdit.getDatacenterIdBits(), bitEdit.getWorkerIdBits(),
              bitEdit.getSeqBits(), bitEdit.isFixedDigitsEnabled());
        }
        LOG.warn(
            "You are running the risk of breaking the global uniqueness of local-guid(Cause you choose the 'manual' strategy) > Consider choose other initialization strategy for local-guid");
        break;
      default:
        break;
    }
  }

}
