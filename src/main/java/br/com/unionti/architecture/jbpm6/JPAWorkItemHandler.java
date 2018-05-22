package br.com.unionti.architecture.jbpm6;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.jbpm.process.workitem.AbstractLogOrThrowWorkItemHandler;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.runtime.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JPAWorkItemHandler extends AbstractLogOrThrowWorkItemHandler implements Cacheable {

	private static final Logger logger = LoggerFactory.getLogger(JPAWorkItemHandler.class);

	public static final String P_RESULT = "Result";
	public static final String P_TYPE = "Type";
	public static final String P_ID = "Id";
	public static final String P_ENTITY = "Entity";
	public static final String P_ACTION = "Action";
	public static final String p_QUERY = "Query";
	public static final String P_QUERY_PARAMS = "QueryParameters";
	public static final String P_QUERY_RESULTS = "QueryResults";
	
	public static final String CREATE_ACTION = "CREATE";
	public static final String UPDATE_ACTION = "UPDATE";
	public static final String GET_ACTION = "GET";
	public static final String DELETE_ACTION = "DELETE";
	public static final String QUERY_ACTION = "QUERY";
	
	private EntityManagerFactory emf;
	private EntityManager em;
	private ClassLoader classloader;
	
	
	public  JPAWorkItemHandler(String persistenceUnit, ClassLoader cl) {
		setLogThrownException(true);
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            this.emf = Persistence.createEntityManagerFactory(persistenceUnit);

        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
        em = emf.createEntityManager();
        this.classloader = cl;
    }
	
		
	public void executeWorkItem(WorkItem wi, WorkItemManager wim) {
		Object actionParam = wi.getParameter(P_ACTION);
		Object entity = wi.getParameter(P_ENTITY);
		Object id = wi.getParameter(P_ID);
		Object type = wi.getParameter(P_TYPE);
		Object queryName = wi.getParameter(p_QUERY);
		Object queryParams = wi.getParameter(P_QUERY_PARAMS);
		Map<String, Object> params = new HashMap<>();
		List<Object> queryResults = Collections.emptyList();
		String action;
		if(actionParam == null) {
			throw new IllegalArgumentException("An action is required. Use 'delete', 'create', 'update', query or 'get'");
		}
		// Only QUERY does no require an entity parameter
		if(entity == null && P_ACTION.equals(QUERY_ACTION)) {
			throw new IllegalArgumentException("An entity is required. Use the 'entity' parameter");
		}
		// join the process transaction
		em.joinTransaction();
		action = String.valueOf(actionParam).trim().toUpperCase();
		logger.info("Action {} on {}", action, entity);
		switch (action) {
		case DELETE_ACTION:
			doDelete(entity);
			break;
		case GET_ACTION:
			if(id == null || type == null) {
				throw new IllegalArgumentException("Id or type can't be null when getting an entity");
			}
			// only works with long for now
			entity = doGet(type.toString(), Long.parseLong(id.toString()));
			break;
		case UPDATE_ACTION:
			doUpdate(entity);
			break;
		case CREATE_ACTION:
			doCreate(entity);
			break;
		case QUERY_ACTION:
			if(queryName == null) {
				throw new IllegalArgumentException("You must provide a '" + p_QUERY + "' parameter to run named queries.");
			}
			queryResults = doQuery(String.valueOf(queryName), queryParams);
			break;
		default:
			throw new IllegalArgumentException("Action " + action + " not recognized. Use 'delete', 'create', 'update', query, or 'get'");
		}
		params.put(P_RESULT, entity);
		params.put(P_QUERY_RESULTS, queryResults);
		wim.completeWorkItem(wi.getId(), params);
	}

	@SuppressWarnings("unchecked")
	private List<Object> doQuery(String queryName, Object queryParams) {
		logger.info("About to run query {}", queryName);
		Map<String, Object> params;
		Query namedQuery = em.createQuery(queryName);
		if(queryParams == null) {
			logger.info("No parameters were provided");
		} else {
			params = ((Map<String, Object>) queryParams);
			logger.info("Parameters {}", params);
			for (Entry<String, Object> param : params.entrySet()) {
				namedQuery.setParameter(param.getKey(), param.getValue());
			}
			// for Java 8 
			//params.forEach(namedQuery::setParameter);
		}
		return namedQuery.getResultList();
	}

	private void doCreate(Object entity) {
		em.persist(entity);
	}

	private Object doUpdate(Object entity) {
		return em.merge(entity);
	}

	private Object doGet(String clazz, Object id) {
		Class<?> type;
		try {
			
			type = Class.forName(clazz, false, classloader);
			//type = classloader.loadClass(clazz);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Can't load type " + clazz);
		}
		return em.find(type, id);
	}

	private void doDelete(Object entity) {
		em.remove(entity);
	}

	public void close() {
		em.clear();
		em.close();
		emf.close();
	}
	
	public void abortWorkItem(WorkItem wi, WorkItemManager wim) {
		wim.abortWorkItem(wi.getId());
	}
}
