package backtester.common;

public class DateUtils {
	static final long MILLIS_IN_A_DAY = 86400000;
	static final long MILLIS_IN_AN_HOUR	 = 3600000;
	static final long MILLIS_IN_A_MINUTE = 60000;
	static final long MILLIS_IN_A_SECOND = 1000;
			
	public static String getBestHumanTimeFromMillis(long millisecondsToFormat) {
		StringBuilder sb = new StringBuilder();
			
		if(millisecondsToFormat >= MILLIS_IN_A_DAY) {
			int days = (int)millisecondsToFormat / (int)MILLIS_IN_A_DAY;
			sb.append(days + " days ");
			millisecondsToFormat = millisecondsToFormat-(days)*MILLIS_IN_A_DAY;
		} 
		
		if(millisecondsToFormat >= MILLIS_IN_AN_HOUR) {
			int hours = (int) millisecondsToFormat / (int) MILLIS_IN_AN_HOUR;
			sb.append(hours + " hours ");
			millisecondsToFormat = millisecondsToFormat-(hours)*MILLIS_IN_AN_HOUR;
		} 
		
		if(millisecondsToFormat >= MILLIS_IN_A_MINUTE) {
			int minutes = (int) millisecondsToFormat / (int) MILLIS_IN_A_MINUTE;
			sb.append(minutes + " minutes ");
			millisecondsToFormat = millisecondsToFormat-(minutes)*MILLIS_IN_A_MINUTE;
		} 
		
		if(millisecondsToFormat >= MILLIS_IN_A_SECOND) {
			int seconds = (int) millisecondsToFormat / (int) MILLIS_IN_A_SECOND;
			sb.append(seconds + " seconds ");
			millisecondsToFormat = millisecondsToFormat-(seconds)*MILLIS_IN_A_SECOND;
		}
		
		if(millisecondsToFormat <= 999) {
			sb.append(millisecondsToFormat + " milliseconds");
		} 
		
		return sb.toString();
	}
}
