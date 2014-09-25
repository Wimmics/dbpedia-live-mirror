package org.dbpedia.extraction.live.mirror.main;

import org.dbpedia.extraction.live.mirror.helper.*;
import org.dbpedia.extraction.live.mirror.iterator.UpdatesIterator;
import org.dbpedia.extraction.live.mirror.sparul.JDBCPoolConnection;
import org.slf4j.Logger;


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 5/24/11
 * Time: 4:26 PM
 * This class is originally created from class defined in http://www.devdaily.com/java/edu/pj/pj010011
 *  which is created by http://www.DevDaily.com
 */
public class Main {


    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Main.class);

    public static void main (String[] args) {
        boolean deleteFiles = false;
        if((Global.options.get("deleteFilesAfterCompletion") != null) &&
                        (Global.options.get("deleteFilesAfterCompletion").compareTo("") != 0))
            deleteFiles = Boolean.parseBoolean(Global.options.get("deleteFilesAfterCompletion"));
        //Initialize logger


        DownloadTimeCounter lastDownload = LastDownloadDateManager.getLastDownloadDate("lastDownloadDate.dat");


        UpdatesIterator iterator = new UpdatesIterator(lastDownload, 3);
        while(iterator.hasNext()){
            DownloadTimeCounter cntr = iterator.next();

            //Since next returns null, if now new updates are available, then we should not go any further,
            //and just wait for mor updates
            if(cntr == null)
                continue;

            //u = new URL("http://dbpedia.aksw.org/updates.live.dbpedia.org/2011/05/22/00/000001.added.nt.gz");
//            String fullFilenameToBeDownloaded = Global.options.get("UpdateServerAddress") + lastDownload.getFormattedFilePath() + ".added.nt.gz";
            //String compressedDownloadedFile = FileDownloader.downloadFile(options.get("UpdateServerAddress") + "2011/05/22/00/000001.added.nt.gz",
            //         options.get("UpdatesDownloadFolder"));

            String addedTriplesFilename = "", deletedTriplesFilename = "";

            addedTriplesFilename = Global.options.get("UpdateServerAddress") + cntr.getFormattedFilePath() +
                                   Global.options.get("addedTriplesFileExtension");

            deletedTriplesFilename = Global.options.get("UpdateServerAddress") + cntr.getFormattedFilePath() +
                                   Global.options.get("removedTriplesFileExtension");

            //Download and decompress the file of deleted triples
            String deletedCompressedDownloadedFile = Utils.downloadFile(deletedTriplesFilename,
                    Global.options.get("UpdatesDownloadFolder"));

            if(deletedCompressedDownloadedFile.compareTo("") != 0){
//                Decompressor.decompressGZipFile(deletedCompressedDownloadedFile, true);
                String decompressedDeletedNTriplesFile = Utils.decompressGZipFile(deletedCompressedDownloadedFile, deleteFiles);

                //Delete triples from Virtuoso graph
                boolean delSuccess = SPARULMediator.deleteFromGraph(decompressedDeletedNTriplesFile, deleteFiles);
                if (delSuccess) {
                    logger.info("Successfully applied DEL patch " + decompressedDeletedNTriplesFile);
                }
                else {
                    logger.error("Error in applying DEL patch " + decompressedDeletedNTriplesFile);
                }

                //Reset the number of failed trails, since the file is found and downloaded successfully
                Global.numberOfSuccessiveFailedTrails = 0;
            }

            //Download and decompress the file of added triples
            String addedCompressedDownloadedFile = Utils.downloadFile(addedTriplesFilename,
                    Global.options.get("UpdatesDownloadFolder"));
            if(addedCompressedDownloadedFile.compareTo("") != 0){
                String decompressedAddedNTriplesFile = Utils.decompressGZipFile(addedCompressedDownloadedFile, deleteFiles);

                //Insert triples into Virtuoso graph
                boolean addSuccess = SPARULMediator.insertIntoGraph(decompressedAddedNTriplesFile, deleteFiles);
                if (addSuccess) {
                    logger.info("Successfully applied ADD patch " + decompressedAddedNTriplesFile);
                }
                else {
                    logger.error("Error in applying ADD patch " + decompressedAddedNTriplesFile);
                }
//                SPARULFormulator.deleteFromGraph(decompressedAddedNTriplesFile, true);

                //Reset the number of failed trails, since the file is found and downloaded successfully
                Global.numberOfSuccessiveFailedTrails = 0;
            }



            //No files with that sequence so that indicates a failed trail, so we increment the counter of unsuccessful queries
            if((addedCompressedDownloadedFile.compareTo("") == 0) && (deletedCompressedDownloadedFile.compareTo("") == 0)){
                Global.numberOfSuccessiveFailedTrails++;
            }
            LastDownloadDateManager.writeLastDownloadDate("lastDownloadDate.dat", cntr.toString());

        }

        JDBCPoolConnection.shutdown();

   }  // end of main

}
