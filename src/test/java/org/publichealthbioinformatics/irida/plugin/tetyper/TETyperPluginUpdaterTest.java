package org.publichealthbioinformatics.irida.plugin.tetyper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.junit.*;
import org.mockito.ArgumentCaptor;
import org.hamcrest.collection.IsMapContaining;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import ca.corefacility.bioinformatics.irida.exceptions.IridaWorkflowException;
import ca.corefacility.bioinformatics.irida.model.sample.MetadataTemplateField;
import ca.corefacility.bioinformatics.irida.model.sample.metadata.MetadataEntry;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SingleEndSequenceFile;
import ca.corefacility.bioinformatics.irida.model.workflow.description.*;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.workflow.IridaWorkflow;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.AnalysisOutputFile;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.AnalysisSubmission;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.Analysis;
import ca.corefacility.bioinformatics.irida.service.sample.SampleService;
import ca.corefacility.bioinformatics.irida.service.sample.MetadataTemplateService;
import ca.corefacility.bioinformatics.irida.service.workflow.IridaWorkflowsService;


public class TETyperPluginUpdaterTest {
    private String WORKFLOW_NAME = "tetyper";
    private String WORKFLOW_VERSION = "0.1.0";

    private TETyperPluginUpdater updater;

    private SampleService sampleService;
    private MetadataTemplateService metadataTemplateService;
    private IridaWorkflowsService iridaWorkflowsService;
    private IridaWorkflow iridaWorkflow;
    private IridaWorkflowDescription iridaWorkflowDescription;

    private UUID uuid = UUID.randomUUID();

    @Before
    public void setUp() throws IridaWorkflowException {
        sampleService = mock(SampleService.class);
        metadataTemplateService = mock(MetadataTemplateService.class);
        iridaWorkflowsService = mock(IridaWorkflowsService.class);
        iridaWorkflow = mock(IridaWorkflow.class);
        iridaWorkflowDescription = mock(IridaWorkflowDescription.class);

        updater = new TETyperPluginUpdater(metadataTemplateService, sampleService, iridaWorkflowsService);

        when(iridaWorkflowsService.getIridaWorkflow(uuid)).thenReturn(iridaWorkflow);
        when(iridaWorkflow.getWorkflowDescription()).thenReturn(iridaWorkflowDescription);
        when(iridaWorkflowDescription.getName()).thenReturn(WORKFLOW_NAME);
        when(iridaWorkflowDescription.getVersion()).thenReturn(WORKFLOW_VERSION);
    }

    @Test
    public void testUpdate() throws Throwable {
        ImmutableMap<String, String> expectedResults = ImmutableMap.<String, String>builder()
                // .put("tetyper/deletions", "none")
                .build();
        Path teTyperSummaryFilePath = Paths.get(ClassLoader.getSystemResource("tetyper_summary.tsv").toURI());

        AnalysisOutputFile teTyperSummaryFile = new AnalysisOutputFile(teTyperSummaryFilePath, null, null, null);
        Analysis analysis = new Analysis(null, ImmutableMap.of("tetyper_summary", teTyperSummaryFile), null, null);
        AnalysisSubmission submission = AnalysisSubmission.builder(uuid)
                .inputFiles(ImmutableSet.of(new SingleEndSequenceFile(null))).build();

        submission.setAnalysis(analysis);

        Sample sample = new Sample();
        sample.setId(0L);

        ImmutableMap<MetadataTemplateField, MetadataEntry> metadataMap = ImmutableMap
                .of(new MetadataTemplateField("", ""), new MetadataEntry("", ""));
        when(metadataTemplateService.getMetadataMap(any(Map.class))).thenReturn(metadataMap);

        updater.update(Lists.newArrayList(sample), submission);

        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);

        //this is the important bit.  Ensures the correct values got pulled from the file
        verify(metadataTemplateService).getMetadataMap(mapCaptor.capture());
        Map<String, MetadataEntry> metadata = mapCaptor.getValue();

        int found = 0;
        for (Map.Entry<String, MetadataEntry> e : metadata.entrySet()) {

            if (expectedResults.containsKey(e.getKey())) {
                String expected = expectedResults.get(e.getKey());

                MetadataEntry value = e.getValue();

                assertEquals("metadata values should match", expected, value.getValue());
                found++;
            }
        }
        assertEquals("should have found the same number of results", expectedResults.keySet().size(), found);

        // this bit just ensures the merged data got saved
        verify(sampleService).updateFields(eq(sample.getId()), mapCaptor.capture());
        Map<MetadataTemplateField, MetadataEntry> value = (Map<MetadataTemplateField, MetadataEntry>) mapCaptor
                .getValue().get("metadata");

        assertEquals(metadataMap.keySet().iterator().next(), value.keySet().iterator().next());
    }

    @Test
    public void testParseTeTyperSummaryFile() throws Throwable {
        Path teTyperSummaryFilePath = Paths.get(ClassLoader.getSystemResource("tetyper_summary.tsv").toURI());
        Map<String, String> teTyperSummary = updater.parseTeTyperSummaryFile(teTyperSummaryFilePath);
            assertThat(teTyperSummary, IsMapContaining.hasKey("deletions"));
            assertThat(teTyperSummary, IsMapContaining.hasKey("structural_variant"));
            assertThat(teTyperSummary, IsMapContaining.hasKey("snps_homozygous"));
            assertThat(teTyperSummary, IsMapContaining.hasKey("snps_heterozygous"));
            assertThat(teTyperSummary, IsMapContaining.hasKey("heterozygous_snp_counts"));
            assertThat(teTyperSummary, IsMapContaining.hasKey("snp_variant"));
            assertThat(teTyperSummary, IsMapContaining.hasKey("combined_variant"));
            assertThat(teTyperSummary, IsMapContaining.hasKey("left_flanks"));
            assertThat(teTyperSummary, IsMapContaining.hasKey("right_flanks"));
            assertThat(teTyperSummary, IsMapContaining.hasKey("left_flank_counts"));
            assertThat(teTyperSummary, IsMapContaining.hasKey("right_flank_counts"));
    }
}