package rs.make.alfresco.actions.resize_image.init;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.evaluator.CompareMimeTypeEvaluator;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.CompositeAction;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.rule.RuleType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import rs.make.alfresco.actions.resize_image.resize.Resize;

public class Init extends BaseScopableProcessorExtension
{
	private NodeService nodeService;
	public NodeService getNodeService() {
		return nodeService;
	}
	public void setNodeService( NodeService nodeService ) {
		this.nodeService = nodeService;
	}

	private ActionService actionService;
	public ActionService getActionService() {
		return actionService;
	}
	public void setActionService( ActionService actionService ) {
		this.actionService = actionService;
	}

	private RuleService ruleService;
	public RuleService getRuleService() {
		return ruleService;
	}
	public void setRuleService( RuleService ruleService ) {
		this.ruleService = ruleService;
	}

	private static Logger logger = Logger.getLogger( Init.class );

	private static final String[] IMAGE_FILE_MIMES = new String[] {
		MimetypeMap.MIMETYPE_IMAGE_GIF,
		MimetypeMap.MIMETYPE_IMAGE_JPEG,
		MimetypeMap.MIMETYPE_IMAGE_PNG,
		MimetypeMap.MIMETYPE_IMAGE_TIFF
	};

	private static final String RULE_NAME_INBOUND = "image-resize-inbound";
	private static final String ACTION_TITLE = "Resize";

	private final QName COMPANY_HOME_QNAME = QName.createQName( NamespaceService.APP_MODEL_1_0_URI, "company_home" );
	private NodeRef companyHome = null;

	private List<StoreRef> stores;
	private List<NodeRef> rootNodes;

	private List<StoreRef> getStores() {
		return this.nodeService.getStores();
	}

	private List<NodeRef> getRootNodes( List<StoreRef> stores ){
		List<NodeRef> rootNodesList = new ArrayList<NodeRef>();
		for( StoreRef store : stores ){
			NodeRef nodeRef = this.nodeService.getRootNode( store );
			rootNodesList.add( nodeRef );
		}
		return rootNodesList;
	}

	private NodeRef resolveCompanyHome( List<NodeRef> rootNodes ) {
		if( companyHome == null ) {
			rootNodes_loop:
			for( NodeRef rootNode : rootNodes ){
				List<ChildAssociationRef> companyHomeAssocRefs = nodeService.getChildAssocs( rootNode , ContentModel.ASSOC_CHILDREN , COMPANY_HOME_QNAME );
				if( companyHomeAssocRefs.size() == 1 ){
					companyHome = companyHomeAssocRefs.get(0).getChildRef();
					break rootNodes_loop;
				}
			}
		}
		return companyHome;
	}

	private void checkStoresAndRootNodes() {
		this.stores = getStores();
		this.rootNodes = getRootNodes( this.stores );
	}

	public void imageResizeActions(){
		checkStoresAndRootNodes();
		NodeRef companyHome = resolveCompanyHome( this.rootNodes );
		imageResizeActions( companyHome );
	}

	public void imageResizeActions( String nodeRef ){
		NodeRef target = new NodeRef( nodeRef );
		if( target != null ) imageResizeActions( target );
	}

	public void imageResizeActions( NodeRef target ){
		List<Rule> existingRules = this.ruleService.getRules( target );
		for( String imageFileMime : IMAGE_FILE_MIMES ){
			String inboundFileName = RULE_NAME_INBOUND + "-" + imageFileMime.replaceAll( "/" ,  "" );
			for( Rule rule : existingRules ){
				if( rule.getTitle().equals( inboundFileName ) ){
					this.ruleService.removeRule( target , rule );
				}
			}
			imageResizeRule( target , RuleType.INBOUND , inboundFileName , true , false , ACTION_TITLE , true , imageFileMime );
		}
	}

	public void imageResizeRule( NodeRef nodeRef , String ruleType , String ruleTitle , boolean applyToChildren , boolean ruleDisabled , String actionTitle , boolean executeAsynchronously , String imageFileMime ){
		Rule rule = new Rule();
		rule.setRuleType( ruleType );
		rule.setTitle( ruleTitle );
		rule.applyToChildren( applyToChildren );
		rule.setRuleDisabled( ruleDisabled );
		rule.setExecuteAsynchronously( executeAsynchronously );

		CompositeAction compositeAction = actionService.createCompositeAction();
		rule.setAction( compositeAction );

		ActionCondition actionCondition = actionService.createActionCondition( CompareMimeTypeEvaluator.NAME );

		Map<String, Serializable> conditionParameters = new HashMap<String, Serializable>(2);
		conditionParameters.put( CompareMimeTypeEvaluator.PARAM_VALUE, imageFileMime );
		conditionParameters.put( CompareMimeTypeEvaluator.PARAM_PROPERTY , ContentModel.PROP_CONTENT );
		actionCondition.setParameterValues( conditionParameters );

		compositeAction.addActionCondition( actionCondition );

		Action resizeAction = actionService.createAction( Resize.NAME );
		resizeAction.setTitle( actionTitle );

		resizeAction.setExecuteAsynchronously( executeAsynchronously );

		compositeAction.addAction( resizeAction );

		if( nodeRef != null ) {
			ruleService.saveRule( nodeRef , rule );
			logger.info( "Rule \"" + ruleTitle + "\" successfully saved aginst node \"" + this.nodeService.getProperty( nodeRef , ContentModel.PROP_NAME ) + "\"" );
		}
	}
}
