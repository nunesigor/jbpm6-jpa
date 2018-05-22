package br.com.unionti.architecture.jbpm6;

import org.jbpm.process.workitem.AbstractLogOrThrowWorkItemHandler;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.runtime.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestWorkItemRequest extends AbstractLogOrThrowWorkItemHandler implements Cacheable {

	private static final Logger logger = LoggerFactory.getLogger(RestWorkItemRequest.class);

	@Override
	public void abortWorkItem(WorkItem arg0, WorkItemManager arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executeWorkItem(WorkItem wi, WorkItemManager wim) {
		wim.abortWorkItem(wi.getId());
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
