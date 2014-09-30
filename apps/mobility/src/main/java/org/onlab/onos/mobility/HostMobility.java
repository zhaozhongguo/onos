package org.onlab.onos.mobility;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.criteria.Criteria.EthCriterion;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.flow.criteria.Criterion.Type;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.host.HostService;
import org.onlab.packet.MacAddress;
import org.slf4j.Logger;

import com.google.common.collect.Lists;


/**
 * Sample reactive forwarding application.
 */
@Component(immediate = true)
public class HostMobility {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = ApplicationId.getAppId();
        hostService.addListener(new InternalHostListener());
        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        flowRuleService.removeFlowRulesById(appId);
        log.info("Stopped");
    }

    public class InternalHostListener
    implements HostListener {

        @Override
        public void event(HostEvent event) {
            switch (event.type()) {
                case HOST_ADDED:
                case HOST_REMOVED:
                case HOST_UPDATED:
                    // don't care if a host has been added, removed.
                    break;
                case HOST_MOVED:
                    log.info("Host {} has moved; cleaning up.", event.subject());
                    cleanup(event.subject());
                    break;

                default:
                    break;

            }

        }

        private void cleanup(Host host) {
            Iterable<Device> devices = deviceService.getDevices();
            List<FlowRule> flowRules = Lists.newLinkedList();
            for (Device device : devices) {
                   flowRules.addAll(cleanupDevice(device, host));
            }
            FlowRule[] flows = new FlowRule[flowRules.size()];
            flows = flowRules.toArray(flows);
            flowRuleService.removeFlowRules(flows);
        }

        private Collection<? extends FlowRule> cleanupDevice(Device device, Host host) {
            List<FlowRule> flowRules = Lists.newLinkedList();
            MacAddress mac = host.mac();
            for (FlowRule rule : flowRuleService.getFlowEntries(device.id())) {
                for (Criterion c : rule.selector().criteria()) {
                    if (c.type() == Type.ETH_DST || c.type() == Type.ETH_SRC) {
                        EthCriterion eth = (EthCriterion) c;
                        if (eth.mac().equals(mac)) {
                            flowRules.add(rule);
                            break;
                        }
                    }
                }
            }
            //TODO: handle ip cleanup
            return flowRules;
        }

    }

}


