package rs.make.alfresco.actions.resize_image.resize;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.imageio.ImageIO;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;

import rs.make.alfresco.alfcontent.AlfContent;

public class Resize extends ActionExecuterAbstractBase
{
	private NodeService nodeService;
	public NodeService getNodeService() {
		return nodeService;
	}
	public void setNodeService( NodeService nodeService ) {
		this.nodeService = nodeService;
	}

	private ContentService contentService;
	public ContentService getContentService() {
		return contentService;
	}
	public void setContentService( ContentService contentService ) {
		this.contentService = contentService;
	}

	protected AlfContent alfContent;
	public AlfContent getAlfContent() {
		return alfContent;
	}
	public void setAlfContent( AlfContent alfContent ) {
		this.alfContent = alfContent;
	}

	private static Logger logger = Logger.getLogger( Resize.class );

	public static final String NAME = "Resize";
	public static final String PARAM_MIME = "compare-mime-type";
	private static final String DEFAULT_IMAGE_TYPE = "jpg";
	private static final String DEFAULT_IMAGE_MIME = MimetypeMap.MIMETYPE_IMAGE_JPEG;
	private static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.toString();
	private static final int[] IMAGE_WIDTHS = new int[] { 640 , 800 , 1024 , 1280 , 1366 , 1600 , 1920 , 2560 , 3840 };

	/**
	 * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(Action, NodeRef)
	 */
	@Override
	public void executeImpl( Action action , NodeRef actionedUponNodeRef ) {
		// Check that the node still exists
		if ( this.nodeService.exists( actionedUponNodeRef ) == true ) {
			try{
				ChildAssociationRef childAssociationef = this.nodeService.getPrimaryParent( actionedUponNodeRef );
				NodeRef parentNodeRef = childAssociationef.getParentRef();
				String name = this.nodeService.getProperty( actionedUponNodeRef , ContentModel.PROP_NAME ).toString();
				String parentName = this.nodeService.getProperty( parentNodeRef , ContentModel.PROP_NAME ).toString();
				logger.info( "Processing \"" + name + "\" in \"" + parentName + "\"." );

				ContentReader actionedUponNodeRefContentReader = this.contentService.getReader( actionedUponNodeRef, ContentModel.PROP_CONTENT );
				BufferedImage originalImage = ImageIO.read( actionedUponNodeRefContentReader.getContentInputStream() );
				int ow = originalImage.getWidth();
				int oh = originalImage.getHeight();
				double ratio = ( (double) ow ) / oh;
				int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

				for( int imgWidth : IMAGE_WIDTHS ){
					if( imgWidth < ow ){
						BufferedImage resizeImageSmoothJpg = resize( originalImage , type , ratio , imgWidth );
						ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
						ImageIO.write( resizeImageSmoothJpg , DEFAULT_IMAGE_TYPE , byteArrayOutputStream );
						ByteArrayInputStream bis = outputToInputStream( byteArrayOutputStream );
						if( bis != null ){
							String newName = imgWidth + "-" + name;
							NodeRef newNodeRef = alfContent.createContentNode( parentNodeRef , newName , bis , DEFAULT_ENCODING , DEFAULT_IMAGE_MIME , null , null );
							if( newNodeRef == null ){
								logger.warn( "Creation of \"" + newName + "\" failed." );
							}
						}
						else{
							logger.warn( "No output stream found in \"" + this.nodeService.getProperty( parentNodeRef , ContentModel.PROP_NAME ) + "\"." );
						}
					}
				}
				logger.info( "Image resize rule executed against \"" + this.nodeService.getProperty( parentNodeRef , ContentModel.PROP_NAME ) + "\"." );
			}
			catch( Exception e ){
				logger.error( "Process of executing image resize rule failed." );
				logger.error( e );
				e.printStackTrace();
			}
			logger.info( "Rule \"" + NAME + "\" applied against node \"" + this.nodeService.getProperty( actionedUponNodeRef , ContentModel.PROP_NAME ) + "\"" );
		}
	}

	/**
	* @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
	*/
	@Override
	protected void addParameterDefinitions( List<ParameterDefinition> paramList )
	{
		// Add definitions for action parameters
	}

	private static BufferedImage resize( BufferedImage originalImage , int type , double ratio , int imgWidth ){
		BufferedImage resizedImage = new BufferedImage( imgWidth, (int) ( imgWidth / ratio ) , type );
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage( originalImage.getScaledInstance( imgWidth , (int) ( imgWidth / ratio ) , Image.SCALE_SMOOTH ) , 0 , 0 , imgWidth , (int) ( imgWidth / ratio ) , null );
		g.dispose();
	
		return resizedImage;
	}
	
	private ByteArrayInputStream outputToInputStream( ByteArrayOutputStream byteArrayOutputStream ){
		if( byteArrayOutputStream == null ) return null;
		ByteArrayInputStream byteArrayInputStream = null;
		try {
			byte[] byteArray = byteArrayOutputStream.toByteArray();
			byteArrayInputStream = new ByteArrayInputStream( byteArray );
			byteArrayOutputStream.close();
		} catch (IOException e) {
			logger.error( "Process of execurting image resize rule failed whilst trying to create image input stream." );
			logger.error( e );
			e.printStackTrace();
		}
		return byteArrayInputStream;
	}
}
