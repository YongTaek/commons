/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.siddhi.test.samples.join;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.wso2.siddhi.api.QueryFactory;
import org.wso2.siddhi.api.eventstream.InputEventStream;
import org.wso2.siddhi.api.eventstream.query.Query;
import org.wso2.siddhi.api.eventstream.query.inputstream.property.WindowType;
import org.wso2.siddhi.api.exception.SiddhiPraserException;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.event.EventImpl;
import org.wso2.siddhi.core.exception.EventStreamNotFoundException;
import org.wso2.siddhi.core.exception.SiddhiException;
import org.wso2.siddhi.core.node.CallbackHandler;
import org.wso2.siddhi.core.node.InputHandler;

import static org.wso2.siddhi.api.condition.where.ConditionOperator.EQUAL;
import static org.wso2.siddhi.api.condition.where.ConditionOperator.GREATERTHAN;


public class Join5TestCase {

    private static final Logger log = Logger.getLogger(Join5TestCase.class);
    private volatile boolean eventCaptured = false;
    private volatile int i = 0;

    @Before
    public void info() {
        log.debug("-----Query processed: Testing aggregation on join with time window Group by having-----");
        eventCaptured = false;
        i = 0;
    }

    @Test
    public void testAPI() throws SiddhiException,
                                 EventStreamNotFoundException, InterruptedException {

        //Instantiate SiddhiManager
        SiddhiManager siddhiManager = new SiddhiManager();
        QueryFactory qf = SiddhiManager.getQueryFactory();

        InputEventStream reserveConfirmationEventStream =
                new InputEventStream("confirmation",
                                     new String[]{"time", "memberId", "payMethod"},
                                     new Class[]{Long.class, Integer.class, String.class}
                );


        InputEventStream requestEventStream =
                new InputEventStream("request",
                                     new String[]{"time", "memberId", "noOfRooms"},
                                     new Class[]{Long.class, Integer.class, Integer.class}
                );


        siddhiManager.addInputEventStream(reserveConfirmationEventStream);
        siddhiManager.addInputEventStream(requestEventStream);

        Query query = qf.createQuery(
                "bought",
                qf.output("id=confirmation.memberId", "payMethod=confirmation.payMethod", "totalRooms=sum(request.noOfRooms)", "rooms=request.noOfRooms"),
                qf.innerJoin(qf.from(reserveConfirmationEventStream).setWindow(WindowType.LENGTH, 50),
                             qf.from(requestEventStream).setWindow(WindowType.TIME, 1000)),
                qf.condition("confirmation.memberId", EQUAL, "request.memberId")
        );
        query.groupBy("confirmation.payMethod");
        query.having(qf.condition("bought.totalRooms", GREATERTHAN, "5"));

        siddhiManager.addQuery(query);
        siddhiManager.addCallback(addCallback());
        siddhiManager.update();
        Thread.sleep(1000);

        sendEvents(siddhiManager);
        assertEvents();
        siddhiManager.shutDownTask();
    }

    @Test
    public void testQuery() throws SiddhiException,
                                   EventStreamNotFoundException, InterruptedException,
                                   SiddhiPraserException {

        //Instantiate SiddhiManager
        SiddhiManager siddhiManager = new SiddhiManager();

        siddhiManager.addConfigurations("confirmation:=timeStamp[long], memberId[int], payMethod[string];" +
                                        "request:=timeStamp[long], memberId[int], noOfRooms[int];" +
                                        "bought:=select id=confirmation.memberId, payMethod=confirmation.payMethod, totalRooms=sum(request.noOfRooms), rooms=request.noOfRooms " +
                                        "           from confirmation[win.length=50],request[win.time=1000] " +
                                        "           where confirmation.memberId==request.memberId " +
                                        "           group by confirmation.payMethod" +
                                        "           having totalRooms>5;");

        siddhiManager.addCallback(addCallback());
        siddhiManager.update();
        Thread.sleep(1000);

        sendEvents(siddhiManager);
        assertEvents();
        siddhiManager.shutDownTask();
    }

    private void assertEvents() throws InterruptedException {
        Thread.sleep(1000);
        Assert.assertTrue(eventCaptured);
        Assert.assertTrue(i == 2);
    }

    private void sendEvents(SiddhiManager siddhiManager) throws SiddhiException {
        InputHandler inputHandlerRequest = siddhiManager.getInputHandler("request");
        InputHandler inputHandlerConfirmation = siddhiManager.getInputHandler("confirmation");

        inputHandlerConfirmation.sendEvent(new EventImpl("confirmation", new Object[]{19, 98, "card"}));
        inputHandlerConfirmation.sendEvent(new EventImpl("confirmation", new Object[]{20, 78, "card"}));
        inputHandlerRequest.sendEvent(new EventImpl("request", new Object[]{21, 199, 2}));
        inputHandlerRequest.sendEvent(new EventImpl("request", new Object[]{22, 19, 2}));

        inputHandlerRequest.sendEvent(new EventImpl("request", new Object[]{23, 101, 2}));
        inputHandlerConfirmation.sendEvent(new EventImpl("confirmation", new Object[]{24, 101, "card"}));
        inputHandlerRequest.sendEvent(new EventImpl("request", new Object[]{25, 102, 5}));
        inputHandlerRequest.sendEvent(new EventImpl("request", new Object[]{26, 103, 1}));
        inputHandlerRequest.sendEvent(new EventImpl("request", new Object[]{27, 104, 3}));

        inputHandlerConfirmation.sendEvent(new EventImpl("confirmation", new Object[]{28, 104, "cash"}));
        inputHandlerConfirmation.sendEvent(new EventImpl("confirmation", new Object[]{29, 103, "card"}));
        inputHandlerRequest.sendEvent(new EventImpl("request", new Object[]{10, 105, 7}));
        inputHandlerConfirmation.sendEvent(new EventImpl("confirmation", new Object[]{31, 102, "cash"}));
        inputHandlerConfirmation.sendEvent(new EventImpl("confirmation", new Object[]{31, 105, "card"}));
    }

    private CallbackHandler addCallback() {
        return new CallbackHandler("bought") {
            public void callBack(Event event) {
                log.debug("       Event captured  " + event + " ");
                if (event.getNthAttribute(1).equals("cash") && (Integer) event.getNthAttribute(2) == 8) {
                    eventCaptured = true;
                }
                if (event.getNthAttribute(1).equals("card") && (Integer) event.getNthAttribute(2) == 10) {
                    eventCaptured = true;
                }
                i++;
            }
        };
    }
}