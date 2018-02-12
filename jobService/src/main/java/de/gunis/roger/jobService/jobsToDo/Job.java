package de.gunis.roger.jobService.jobsToDo;

public class Job {
    String name = "";
    private String jobProposal = "";

    public Job(String name) {
        this.name = name;
    }

    public Job(String name, String jobProposal) {
        this.name = name;
        this.jobProposal = jobProposal;
    }

    public String getName() {
        return name;
    }

    public String getJobProposal() {
        return jobProposal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Job job = (Job) o;

        return name != null ? name.equals(job.name) : job.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        String addProposal = "";
        if (!jobProposal.equals("")) {

            addProposal = ", jobProposal='" + jobProposal + '\'';
        }
        return "Job{" +
                "name='" + name + '\'' + addProposal +
                '}';
    }
}
