package org.publichealthbioinformatics.irida.plugin.tetyper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import ca.corefacility.bioinformatics.irida.exceptions.IridaWorkflowNotFoundException;
import ca.corefacility.bioinformatics.irida.exceptions.PostProcessingException;
import ca.corefacility.bioinformatics.irida.model.sample.MetadataTemplateField;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.sample.metadata.MetadataEntry;
import ca.corefacility.bioinformatics.irida.model.sample.metadata.PipelineProvidedMetadataEntry;
import ca.corefacility.bioinformatics.irida.model.workflow.IridaWorkflow;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.AnalysisOutputFile;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.type.AnalysisType;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.AnalysisSubmission;
import ca.corefacility.bioinformatics.irida.pipeline.results.updater.AnalysisSampleUpdater;
import ca.corefacility.bioinformatics.irida.service.sample.MetadataTemplateService;
import ca.corefacility.bioinformatics.irida.service.sample.SampleService;
import ca.corefacility.bioinformatics.irida.service.workflow.IridaWorkflowsService;

/**
 * This implements a class used to perform post-processing on the analysis
 * pipeline results to extract information to write into the IRIDA metadata
 * tables. Please see
 * <https://github.com/phac-nml/irida/blob/development/src/main/java/ca/corefacility/bioinformatics/irida/pipeline/results/AnalysisSampleUpdater.java>
 * or the README.md file in this project for more details.
 */
public class TETyperPluginUpdater implements AnalysisSampleUpdater {

	private final MetadataTemplateService metadataTemplateService;
	private final SampleService sampleService;
	private final IridaWorkflowsService iridaWorkflowsService;

	/**
	 * Builds a new {@link TETyperPluginUpdater} with the given services.
	 * 
	 * @param metadataTemplateService The metadata template service.
	 * @param sampleService           The sample service.
	 * @param iridaWorkflowsService   The irida workflows service.
	 */
	public TETyperPluginUpdater(MetadataTemplateService metadataTemplateService, SampleService sampleService,
								IridaWorkflowsService iridaWorkflowsService) {
		this.metadataTemplateService = metadataTemplateService;
		this.sampleService = sampleService;
		this.iridaWorkflowsService = iridaWorkflowsService;
	}

	/**
	 * Code to perform the actual update of the {@link Sample}s passed in the
	 * collection.
	 * 
	 * @param samples  A collection of {@link Sample}s that were passed to this
	 *                 pipeline.
	 * @param analysis The {@link AnalysisSubmission} object corresponding to this
	 *                 analysis pipeline.
	 */
	@Override
	public void update(Collection<Sample> samples, AnalysisSubmission analysis) throws PostProcessingException {
		if (samples == null) {
			throw new IllegalArgumentException("samples is null");
		} else if (analysis == null) {
			throw new IllegalArgumentException("analysis is null");
		} else if (samples.size() != 1) {
			// In this particular pipeline, only one sample should be run at a time so I
			// verify that the collection of samples I get has only 1 sample
			throw new IllegalArgumentException(
					"samples size=" + samples.size() + " is not 1 for analysisSubmission=" + analysis.getId());
		}

		// extract the 1 and only sample (if more than 1, would have thrown an exception
		// above)
		final Sample sample = samples.iterator().next();

		// extracts paths to the analysis result files
		AnalysisOutputFile teTyperSummaryAnalysisOutputFile = analysis.getAnalysis().getAnalysisOutputFile("tetyper_summary");
		Path teTyperSummaryFilePath = teTyperSummaryAnalysisOutputFile.getFile();

		try {
			Map<String, MetadataEntry> metadataEntries = new HashMap<>();

			// get information about the workflow (e.g., version and name)
			IridaWorkflow iridaWorkflow = iridaWorkflowsService.getIridaWorkflow(analysis.getWorkflowId());
			String workflowVersion = iridaWorkflow.getWorkflowDescription().getVersion();
			String workflowName = iridaWorkflow.getWorkflowDescription().getName();

			// gets information from the "tetyper_summary.tsv" output file and constructs metadata
			// objects
			Map<String, String> teTyperSummary = parseTeTyperSummaryFile(teTyperSummaryFilePath);

			String deletions = teTyperSummary.get("deletions");
			// add
			PipelineProvidedMetadataEntry teTyperDeletionsEntry = new PipelineProvidedMetadataEntry(deletions, "string", analysis);

			// key will be string like 'tetyper/deletions'
			String key = workflowName + "/deletions";
			// Not adding any metadata entries yet....
			// metadataEntries.put(key, teTyperDeletionsEntry);

			Map<MetadataTemplateField, MetadataEntry> metadataMap = metadataTemplateService.getMetadataMap(metadataEntries);

			// merges with existing sample metadata
			sample.mergeMetadata(metadataMap);

			// does an update of the sample metadata
			sampleService.updateFields(sample.getId(), ImmutableMap.of("metadata", sample.getMetadata()));
		} catch (IOException e) {
			throw new PostProcessingException("Error parsing gene detection status file", e);
		} catch (IridaWorkflowNotFoundException e) {
			throw new PostProcessingException("Could not find workflow for id=" + analysis.getWorkflowId(), e);
		}
	}


	/**
	 * Parses out values from the hash file into a {@link List<Map>} linking 'gene_name' to 'detection_status'.
	 * 
	 * @param teTyperSummaryFilePath The {@link Path} to the file containing the hash values from
	 *                 the pipeline. This file should contain contents like:
	 * 
	 *                 <pre>
	 * Deletions	Structural_variant	SNPs_homozygous	SNPs_heterozygous	Heterozygous_SNP_counts	SNP_variant	Combined_variant	Left_flanks	Right_flanks	Left_flank_counts	Right_flank_counts
	 * none 	Tn4401b 	C8015T 	none 	none 	2 	Tn4401b-2 	AGATA|GTTCT 	AGATA|GTTCT 	56|151 	52|101
	 *                 </pre>
	 * 
	 * @return An {@link List<Map>} linking 'geneName' to 'detectionStatus'.
	 * @throws IOException             If there was an error reading the file.
	 * @throws PostProcessingException If there was an error parsing the file.
	 */
	@VisibleForTesting
	Map<String, String> parseTeTyperSummaryFile(Path teTyperSummaryFilePath) throws IOException, PostProcessingException {
		Map<String, String> teTyperSummary = new HashMap<>();

		BufferedReader teTyperSummaryReader = new BufferedReader(new FileReader(teTyperSummaryFilePath.toFile()));

		try {
			String headerLine = teTyperSummaryReader.readLine();

			String[] fieldNames = headerLine.toLowerCase().split("\t");
			String teTyperSummaryLine = teTyperSummaryReader.readLine();
			String[] teTyperSummaryEntries = teTyperSummaryLine.split("\t");
			for (int i = 0; i < fieldNames.length; i++) {
				teTyperSummary.put(fieldNames[i], teTyperSummaryEntries[i]);
			}
		} finally {
			// make sure to close, even in cases where an exception is thrown
			teTyperSummaryReader.close();
		}

		return teTyperSummary;
	}

	/**
	 * The {@link AnalysisType} this {@link AnalysisSampleUpdater} corresponds to.
	 * 
	 * @return The {@link AnalysisType} this {@link AnalysisSampleUpdater}
	 *         corresponds to.
	 */
	@Override
	public AnalysisType getAnalysisType() {
		return TETyperPlugin.TETYPER;
	}
}
