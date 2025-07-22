package capstone_project.config.expired;

import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OtpSchedulerService {

    private final Scheduler scheduler;

    public void scheduleOtpExpirationJob(String email, String otp) throws SchedulerException {
        JobDetail jobDetail = buildJobDetail(email, otp);
        Trigger trigger = buildJobTrigger(jobDetail);

        JobKey jobKey = new JobKey("expireOtpJob-" + email);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }

        scheduler.scheduleJob(jobDetail, trigger);
    }

    private JobDetail buildJobDetail(String email, String otp) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("email", email);
        jobDataMap.put("otp", otp);

        return JobBuilder.newJob(ExpiredOtpJob.class)
                .withIdentity("expireOtpJob-" + email)
                .withDescription("Expire OTP Job")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName() + "-trigger")
                .withDescription("Expire OTP Trigger")
                .startAt(DateBuilder.futureDate(15, DateBuilder.IntervalUnit.MINUTE))
                .build();
    }
}
