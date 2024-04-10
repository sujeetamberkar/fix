import gradingTools.comp533s24.assignment01.S24Assignment0_1Suite;
import gradingTools.comp533s24.assignment01.S24Assignment0_1SuiteNoHints;
import gradingTools.comp533s24.assignment01.S24Assignment0_1SuiteNoHints2;
import trace.grader.basics.GraderBasicsTraceUtility;

public class TestConcurrentOddNumbers {
	public static void main(String[] args) {
		OddNumbersUtil.setDoTests(true);
//		// if you want to step through local checks calls to your code
//		// set this to false
//		BasicProjectExecution.setUseMethodAndConstructorTimeOut(false);
		
		// if you set this to false, grader steps will not be traced
		GraderBasicsTraceUtility.setTracerShowInfo(false);
		
//		// if you set this to false, all grader steps will be traced,
//		// not just the ones that failed
		GraderBasicsTraceUtility.setBufferTracedMessages(true);
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

//		S24Assignment0_1SuiteNoHints.main(args);
		S24Assignment0_1SuiteNoHints2.main(args);

	}
}
