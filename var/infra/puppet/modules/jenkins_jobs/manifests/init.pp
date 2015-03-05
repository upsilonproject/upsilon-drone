class jenkins_jobs {
    package { 'jenkins_jobs':
        ensure => 'installed',
        provider = 'pip',
    }
}
