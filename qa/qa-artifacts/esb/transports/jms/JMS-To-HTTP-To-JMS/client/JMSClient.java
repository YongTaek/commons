package com.wso2.jms.service;
/*  Copyright (c) WSO2 Inc. (http://wso2.com) All Rights Reserved.

  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*
*/

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

import java.rmi.RemoteException;

public class JMSClient {

    public static void main(String[] args) throws RemoteException {
        OMElement payload = createonewayPayload();
        ServiceClient serviceclient;

        ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem("/home/evanthika/WSO2/CARBON/QA/TRAINING/ESB/wso2esb-4.0.3/samples/axis2Client/client_repo/", "/home/evanthika/WSO2/CARBON/QA/TRAINING/ESB/wso2esb-4.0.3/samples/axis2Client/client_repo/conf/axis2.xml");
        Options opt = new Options();
        opt.setTo(new EndpointReference ("jms:/dynamicQueues/JMSTempQueue?transport.jms.DestinationType=queue&transport.jms.ContentTypeProperty=contentType&java.naming.provider.url=tcp://localhost:61616&java.naming.factory.initial=org.apache.activemq.jndi.ActiveMQInitialContextFactory&transport.jms.ConnectionFactoryType=queue&transport.jms.ConnectionFactoryJNDIName=QueueConnectionFactory&transport.jms.Destination=dynamicQueues/JMSTempQueue"));
        opt.setAction("urn:placeOrder");
        serviceclient = new ServiceClient(cc, null);
        serviceclient.setOptions(opt);
        serviceclient.fireAndForget(payload);

    }

    public static OMElement createonewayPayload(){
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://services.samples", "ns");
        OMElement purchase = fac.createOMElement("placeOrder", omNs);
        OMElement request = fac.createOMElement("order", omNs);
        OMElement symbol = fac.createOMElement("symbol", omNs);
        OMElement quantity = fac.createOMElement("quantity", omNs);
        OMElement price = fac.createOMElement("price", omNs);

        symbol.setText("MSFT");
        price.setText("145.222");
        quantity.setText("1000");


        purchase.addChild(request);
        request.addChild(symbol);
        request.addChild(price);
        request.addChild(quantity);

        return purchase;
    }
}

