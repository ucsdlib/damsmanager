package edu.ucsd.library.xdre.utils;

import java.util.Date;

/**
 * A submitted request for processing
 * @author lsitu
 *
 */
public class Submission {
		public static enum Status {done, progressing, error, interrupted};
		private Thread worker = null; //Servlet, worker thread
		private String submissionId = null; //Id of the submission
		private String owner = null; //Owner of the submission
		private Status status = null; //Status of the submission
		private Date submittedDate = null; //Time submitted
		private Date startDate = null; //Time the process started.
		private Date endDate = null; //Time the process ended
		private String requestInfo = null;
		private String message = null;
		
		public Submission (Thread worker, String submissionId, String owner){
			this.worker = worker;
			this.submissionId = submissionId;
			this.owner = owner;
		}

		public Thread getWorker() {
			return worker;
		}

		public void setWorker(Thread worker) {
			this.worker = worker;
		}

		public String getSubmissionId() {
			return submissionId;
		}

		public void setSubmissionId(String submissionId) {
			this.submissionId = submissionId;
		}

		public String getOwner() {
			return owner;
		}

		public void setOwner(String owner) {
			this.owner = owner;
		}

		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public Date getSubmittedDate() {
			return submittedDate;
		}

		public void setSubmittedDate(Date submittedDate) {
			this.submittedDate = submittedDate;
		}

		public Date getStartDate() {
			return startDate;
		}

		public void setStartDate(Date startDate) {
			this.startDate = startDate;
		}

		public Date getEndDate() {
			return endDate;
		}

		public void setEndDate(Date endDate) {
			this.endDate = endDate;
		}

		public String getRequestInfo() {
			return requestInfo;
		}

		public void setRequestInfo(String requestInfo) {
			this.requestInfo = requestInfo;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
}
