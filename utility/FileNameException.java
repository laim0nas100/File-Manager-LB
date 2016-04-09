/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileNameException extends Exception {
    public FileNameException(){}

		public FileNameException(String message){
			super(message);
		}

		public FileNameException(Throwable cause){
			super(cause);
		}

		public FileNameException(String message, Throwable cause){
			super(message, cause);
		}

		public FileNameException(String message,
                                    Throwable cause, 
                                    boolean enableSuppression, 
                                    boolean writableStackTrace){
			super(message, cause, enableSuppression, writableStackTrace);
		}
}
