/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.wunderboss.messaging.hornetq;

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.codecs.Codecs;
import org.projectodd.wunderboss.messaging.Context;
import org.projectodd.wunderboss.messaging.Destination.ListenOption;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.MessageHandler;

import javax.jms.JMSConsumer;
import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.List;

public class MessageHandlerGroup implements Listener {

    public MessageHandlerGroup(HQSpecificContext context,
                               MessageHandler handler,
                               Codecs codecs,
                               HQDestination destination,
                               Options<ListenOption> options) {
        this.context = context;
        this.handler = handler;
        this.codecs = codecs;
        this.destination = destination;
        this.options = options;
    }

    public synchronized MessageHandlerGroup start() throws Exception {
        if (!this.started) {
            Integer option = this.options.getInt(ListenOption.CONCURRENCY);
            int concurrency = option != null ? option : this.destination.defaultConcurrency();
            log.info("Starting listener for '" + this.destination.name() + "' concurrency=" + concurrency);
            while(concurrency-- > 0) {
                HQSpecificContext subContext =
                        this.context.createChildContext((Context.Mode)this.options.get(ListenOption.MODE));
                listeners.add((new JMSListener(this.handler,
                                               this.codecs,
                                               this.destination,
                                               subContext,
                                               createConsumer(subContext)))
                                      .start());
            }

            this.started = true;
        }

        return this;
    }

    @Override
    public synchronized void close() throws Exception {
        if (this.started) {
            this.started = false;
            this.context.close();
            for(JMSListener each : this.listeners) {
                each.stop();
            }
            this.listeners.clear();
        }
    }

    protected JMSConsumer createConsumer(HQSpecificContext context) throws JMSException {
        String selector = this.options.getString(ListenOption.SELECTOR);
        javax.jms.Destination destination = this.destination.jmsDestination();

        return context.jmsContext().createConsumer(destination, selector);
    }

    private final MessageHandler handler;
    private final Codecs codecs;
    private final HQDestination destination;
    private final Options<ListenOption> options;
    private final HQSpecificContext context;
    private final List<JMSListener> listeners = new ArrayList<>();
    private boolean started = false;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.messaging");
}
