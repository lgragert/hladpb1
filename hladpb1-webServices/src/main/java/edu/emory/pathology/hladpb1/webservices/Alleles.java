package edu.emory.pathology.hladpb1.webservices;

import edu.emory.pathology.hladpb1.imgtdb.AlleleFinder;
import edu.emory.pathology.hladpb1.imgtdb.HypervariableRegionFinder;
import edu.emory.pathology.hladpb1.imgtdb.data.Allele;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * This class implements the Alleles RESTful web services.
 * 
 * @author ghsmith
 */
@Path("alleles")
public class Alleles {

    // These are set by SessionFilter.
    protected static ThreadLocal<AlleleFinder> alleleFinder = new ThreadLocal<>();
    protected static ThreadLocal<HypervariableRegionFinder> hypervariableRegionFinder = new ThreadLocal<>();

    @GET
    @Produces("application/json")
    public List<Allele> getJson(@QueryParam("synonymous") String synonymous, @QueryParam("sab") String sab, @QueryParam("hvrMatchCount") int matchesHvrCount) {
        List<Allele> alleles = alleleFinder.get().getAlleleList();
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
    public Allele getJsonAllele(@PathParam("alleleName") String alleleName) {
        return alleleFinder.get().getAllele(alleleName);
    }

    @PUT
    @Path("{alleleName}")
    @Consumes("application/json")
    public void putJsonAllele(@PathParam("alleleName") String alleleName, Allele updateAllele) {
        Allele allele = alleleFinder.get().getAllele(alleleName);
        boolean[] assignCompatibilityStatus = new boolean[] { false }; // wrapping for use in lambda
        if(!updateAllele.getDonorTypeForCompat().equals(allele.getDonorTypeForCompat())) {
            allele.setDonorTypeForCompat(updateAllele.getDonorTypeForCompat());
            assignCompatibilityStatus[0] = true;
        }
        if(!updateAllele.getRecipientTypeForCompat().equals(allele.getRecipientTypeForCompat())) {
            allele.setRecipientTypeForCompat(updateAllele.getRecipientTypeForCompat());
            assignCompatibilityStatus[0] = true;
        }
        if(!updateAllele.getRecipientAntibodyForCompat().equals(allele.getRecipientAntibodyForCompat())) {
            // Not allowing antibodies to specified for alleles that are not the
            // subject of a single antigen bead.
            if(allele.getSingleAntigenBead()) {
                allele.setRecipientAntibodyForCompat(updateAllele.getRecipientAntibodyForCompat());
                assignCompatibilityStatus[0] = true;
            }
        }
        if(assignCompatibilityStatus[0]) {
            alleleFinder.get().computeCompatInterpretation(hypervariableRegionFinder.get());
        }
        if(updateAllele.getReferenceForMatches()) {
            // This will concurrently un-assign the current reference allele.
            alleleFinder.get().assignHypervariableRegionVariantMatches(alleleName);
        }
    }
    
}
