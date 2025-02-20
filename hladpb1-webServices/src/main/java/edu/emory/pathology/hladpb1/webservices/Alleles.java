package edu.emory.pathology.hladpb1.webservices;

import edu.emory.pathology.hladpb1.imgtdb.data.Allele;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.commons.lang.SerializationUtils;

/**
 * This class implements the Alleles RESTful web services.
 * 
 * @author ghsmith
 */
@Path("alleles")
public class Alleles {

    @GET
    @Produces("application/json")
    public List<Allele> getJson(@QueryParam("noCodons") String noCodons, @QueryParam("synonymous") String synonymous, @QueryParam("sab") String sab, @QueryParam("hvrMatchCount") int matchesHvrCount) {
        List<Allele> alleles = SessionFilter.alleleFinder.get().getAlleleList();
        // noCodons saves bandwidth
        if("true".equals(noCodons)) {
            alleles = (List)SerializationUtils.clone((Serializable)alleles);
            alleles.stream().forEach((allele) -> { allele.setCodonMap(null); });
        }
        // Implementing some rudimentary filtering.
        if("false".equals(synonymous)) {
            alleles = alleles.stream().filter((allele) -> (allele.getSynonymousAlleleName() == null)).collect(Collectors.toList());
        }
        if("true".equals(sab)) {
            alleles = alleles.stream().filter((allele) -> (allele.getSingleAntigenBead())).collect(Collectors.toList());
        }
        if(matchesHvrCount > 0) {
            alleles = alleles.stream().filter((allele) -> (allele.getMatchesHvrCount() >= matchesHvrCount)).collect(Collectors.toList());
        }
        return alleles;
    }

    @GET
    @Path("{alleleName}")
    @Produces("application/json")
    public Allele getJsonAllele(@PathParam("alleleName") String alleleName, @QueryParam("noCodons") String noCodons) {
        Allele allele = SessionFilter.alleleFinder.get().getAllele(alleleName);
        // noCodons saves bandwidth
        if("true".equals(noCodons)) {
            allele = (Allele)SerializationUtils.clone((Serializable)allele);
            allele.setCodonMap(null);
        }
        return allele;
    }

    @PUT
    @Path("{alleleName}")
    @Consumes("application/json")
    public void putJsonAllele(@PathParam("alleleName") String alleleName, @QueryParam("enumeratedAlleleMode") String enumeratedAlleleMode, Allele updateAllele) {

        synchronized(SessionFilter.sessionMutex.get()) {

            Allele allele = SessionFilter.alleleFinder.get().getAllele(alleleName);

            // 1. Set the reference allele for the difference report.
            if(updateAllele.getReferenceForMatches()) {
                // This will concurrently un-assign the current reference allele.
                SessionFilter.alleleFinder.get().assignHypervariableRegionVariantMatches(alleleName);
            }
            
            // 2. Set the donor, recipient, and recipient antibody types for a
            //    full compatibility evaluation.
            allele.setDonorTypeForCompat(updateAllele.getDonorTypeForCompat());
            allele.setRecipientTypeForCompat(updateAllele.getRecipientTypeForCompat());
            // Not allowing antibodies to specified for alleles that are not the
            // subject of a single antigen bead.
            if(allele.getSingleAntigenBead()) {
                allele.setRecipientAntibodyForCompat(updateAllele.getRecipientAntibodyForCompat());
            }

            // 3. If the donor/recipient allele #1/#2 fields are being used by
            //    a client that cares to keep track of what is allele #1 and
            //    what is allele #2, then process those properties.
            if("true".equals(enumeratedAlleleMode)) {
                allele.setDonorAllele1(updateAllele.getDonorAllele1());
                if(allele.getDonorAllele1() != null && allele.getDonorAllele1()) {
                    SessionFilter.alleleFinder.get().getAlleleList().stream().filter((loopAllele) -> (allele != loopAllele)).forEach((loopAllele) -> { loopAllele.setDonorAllele1(false); });
                }
                allele.setDonorAllele2(updateAllele.getDonorAllele2());
                if(allele.getDonorAllele2() != null && allele.getDonorAllele2()) {
                    SessionFilter.alleleFinder.get().getAlleleList().stream().filter((loopAllele) -> (allele != loopAllele)).forEach((loopAllele) -> { loopAllele.setDonorAllele2(false); });
                }
                allele.setRecipientAllele1(updateAllele.getRecipientAllele1());
                if(allele.getRecipientAllele1() != null && allele.getRecipientAllele1()) {
                    SessionFilter.alleleFinder.get().getAlleleList().stream().filter((loopAllele) -> (allele != loopAllele)).forEach((loopAllele) -> { loopAllele.setRecipientAllele1(false); });
                }
                allele.setRecipientAllele2(updateAllele.getRecipientAllele2());
                if(allele.getRecipientAllele2() != null && allele.getRecipientAllele2()) {
                    SessionFilter.alleleFinder.get().getAlleleList().stream().filter((loopAllele) -> (allele != loopAllele)).forEach((loopAllele) -> { loopAllele.setRecipientAllele2(false); });
                }
                SessionFilter.alleleFinder.get().getAlleleList().stream().forEach((loopAllele) -> {
                    loopAllele.setDonorTypeForCompat(false);
                    if((loopAllele.getDonorAllele1() != null && loopAllele.getDonorAllele1()) || (loopAllele.getDonorAllele2() != null && loopAllele.getDonorAllele2())) {
                        loopAllele.setDonorTypeForCompat(true);
                    }
                    loopAllele.setRecipientTypeForCompat(false);
                    if((loopAllele.getRecipientAllele1() != null && loopAllele.getRecipientAllele1()) || (loopAllele.getRecipientAllele2() != null && loopAllele.getRecipientAllele2())) {
                        loopAllele.setRecipientTypeForCompat(true);
                    }
                });
            }

            // 4. Do the compatibility evaluation.
            SessionFilter.alleleFinder.get().computeCompatInterpretation(SessionFilter.hypervariableRegionFinder.get());

        }
        
    }
    
}
