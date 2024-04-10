import grader.basics.testcase.PassFailJUnitTestCase;
import gradingTools.comp533s24.assignment01.S24Assignment0_1Suite;
import gradingTools.comp533s24.assignment01.S24Assignment0_1SuiteNoHints;
import gradingTools.shared.testcases.concurrency.oddNumbers.hints.AbstractHint;
import gradingTools.shared.testcases.concurrency.oddNumbers.hints.ForkJoinHintsSuite;
import gradingTools.shared.testcases.concurrency.oddNumbers.hints.SynchronizationHintsSuite;
import trace.grader.basics.GraderBasicsTraceUtility;

public class ShowSynchronizationHints {
	public static void main(String[] args) {
		OddNumbersUtil.setDoTests(true);
		OddNumbersUtil.setTraceSynchronization(true);
//		// if you want to step through local checks calls to your code
//		// set this to false
//		BasicProjectExecution.setUseMethodAndConstructorTimeOut(false);
		
		// if you set this to false, grader steps will not be traced
		GraderBasicsTraceUtility.setTracerShowInfo(false);
		
//		// if you set this to false, all grader steps will be traced,
//		// not just the ones that failed
		GraderBasicsTraceUtility.setBufferTracedMessages(false);
		
		GraderBasicsTraceUtility.setPrintPreTestAnnouncement(false);
		
		// Change this to false if you want a hint to printed regardless of whether the
		// hint is needed to correct the associated test
		AbstractHint.setCheckIfPrecedingTestIsCorrect(true);
		// Change this to true if you want a hint to be executed only after all
		// the previous hints have been executed in previous test cases.
		// This will slow the execution of hints, so students wi reflect on them
		AbstractHint.setCheckIfPrecedingHintHasBeenExecuted(false);
//		
//		// Change this number if a test trace gets longer than 600 and is clipped
//		GraderBasicsTraceUtility.setMaxPrintedTraces(600);
//		
//		// Change this number if all traces together are longer than 2000
//		GraderBasicsTraceUtility.setMaxTraces(2000);
		
//		// Change this number if your process times out prematurely
//		BasicProjectExecution.setProcessTimeOut(5);
//		
		// You need to always call such a method
		
		GraderBasicsTraceUtility.setPrintPreTestAnnouncement(false);
		SynchronizationHintsSuite.main(args);
//		F23Assignment0_1Suite.main(args);
//		S24Assignment0Suite.main(args);
//		ForkJoinHintsSuite.main(args);
	}
}
