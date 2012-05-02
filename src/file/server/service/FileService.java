package file.server.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sun.jersey.api.NotFoundException;

import file.server.model.bean.Metadata;
import file.server.model.bean.Revision;
import file.server.model.dao.MetadataDAO;
import file.server.model.dao.RevisionDAO;

@Service
public class FileService {

	@Autowired
	private MetadataDAO metadataDAO;

	@Autowired
	private RevisionDAO revisionDAO;

	/**
	 * Upload file, if this file already exists and overwrite is true, then a
	 * new revision of file will be generated
	 * 
	 * @param file
	 * @param metadata
	 * @return
	 */
	public Metadata upload(InputStream file, Metadata metadata, boolean overwrite)
			throws Exception {

		Metadata existsMeta = metadataDAO.findByPathAndName(metadata.getPath(),
				metadata.getName());

		if (existsMeta == null) {
			metadata.createRevision();
			metadataDAO.insert(metadata);
			revisionDAO.insert(metadata.createRevision());
			metadata.updateCurrentRevision();
			metadataDAO.update(metadata);
		} else if (overwrite) {
			revisionDAO.loadRevisions(existsMeta);
			revisionDAO.insert(existsMeta.createRevision());
			existsMeta.updateCurrentRevision();
			metadataDAO.update(existsMeta);

			metadata = existsMeta;
		} else {
			// nothing to do
			throw new FileNotFoundException();
		}

		// save file
		String pathToSaveFile = GedFileUtil.getPathById(metadata.getCurrentRevision());
		
		writeToFile(file, new File(pathToSaveFile));

		return metadata;

	}

	/**
	 * Load file
	 * 
	 * @param id 
	 * @param revision
	 * @return
	 * @throws Exception 
	 */
	public Metadata load(Long id, Long revision) throws Exception {
		Metadata retVal = metadataDAO.get(id);
		Revision rev = null;
		File file = null;
		Long revisionId;
		
		if(retVal != null) {
			
			//if sent revision and thats not the current revision, then load that revision
			if(revision > 0 && retVal.getCurrentRevision() != revision) {
				rev = revisionDAO.get(revision);
				if(rev.getMetadataId() == retVal.getId()) {
					revisionId = rev.getId();
				} else {
					throw new NotFoundException("invalid revision");
				}
			
			} else {
				revisionId = retVal.getCurrentRevision();
			}
		
			file = new File(GedFileUtil.getPathById(revisionId));
			retVal.setFile(file);
		}

		return retVal;
	}

	public Metadata getRevisions(long id) throws Exception {
		
		Metadata metadata = metadataDAO.get(id);
		revisionDAO.loadRevisions(metadata);
				
		return metadata;
	}
	
	
	private void writeToFile(InputStream uploadedInputStream,
			File uploadedFileLocation) {

		try {
			uploadedFileLocation.getParentFile().mkdirs();
			
			OutputStream out = new FileOutputStream(uploadedFileLocation);
			int read = 0;
			byte[] bytes = new byte[1024];

			out = new FileOutputStream(uploadedFileLocation);
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	public List<Metadata> loadByPath(String path) throws Exception {
		return metadataDAO.findByPath(path);
	}
	

}
