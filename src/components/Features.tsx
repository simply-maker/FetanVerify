import React from 'react';
import { Shield, Zap, Globe, Lock, CheckCircle, Clock } from 'lucide-react';

export function Features() {
  const features = [
    {
      icon: Shield,
      title: 'Bank-Level Security',
      description: 'Advanced encryption and security protocols to protect your sensitive data.',
      color: 'text-primary-600',
      bgColor: 'bg-primary-100',
    },
    {
      icon: Zap,
      title: 'Lightning Fast',
      description: 'Get verification results in under 30 seconds with our optimized processing.',
      color: 'text-warning-600',
      bgColor: 'bg-warning-100',
    },
    {
      icon: Globe,
      title: 'Global Coverage',
      description: 'Support for documents from 190+ countries with local compliance.',
      color: 'text-success-600',
      bgColor: 'bg-success-100',
    },
    {
      icon: Lock,
      title: 'Privacy First',
      description: 'Your data is encrypted, never stored permanently, and fully compliant with GDPR.',
      color: 'text-error-600',
      bgColor: 'bg-error-100',
    },
    {
      icon: CheckCircle,
      title: '99.9% Accuracy',
      description: 'Industry-leading accuracy rates powered by advanced AI and machine learning.',
      color: 'text-success-600',
      bgColor: 'bg-success-100',
    },
    {
      icon: Clock,
      title: '24/7 Support',
      description: 'Round-the-clock customer support to help you with any questions or issues.',
      color: 'text-primary-600',
      bgColor: 'bg-primary-100',
    },
  ];

  return (
    <section className="py-20 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-16">
          <h2 className="text-3xl lg:text-4xl font-bold text-gray-900 mb-4">
            Why Choose FetanVerify?
          </h2>
          <p className="text-xl text-gray-600 max-w-3xl mx-auto">
            Built with enterprise-grade security and designed for seamless user experience
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
          {features.map((feature, index) => (
            <div
              key={index}
              className="group card p-8 hover:shadow-lg transition-all duration-300 hover:-translate-y-1"
            >
              <div className={`w-12 h-12 ${feature.bgColor} rounded-lg flex items-center justify-center mb-6 group-hover:scale-110 transition-transform duration-200`}>
                <feature.icon className={`w-6 h-6 ${feature.color}`} />
              </div>
              
              <h3 className="text-xl font-semibold text-gray-900 mb-3">
                {feature.title}
              </h3>
              
              <p className="text-gray-600 leading-relaxed">
                {feature.description}
              </p>
            </div>
          ))}
        </div>

        {/* CTA Section */}
        <div className="mt-16 text-center">
          <div className="bg-gradient-to-r from-primary-600 to-primary-700 rounded-2xl p-8 lg:p-12">
            <h3 className="text-2xl lg:text-3xl font-bold text-white mb-4">
              Ready to Get Started?
            </h3>
            <p className="text-primary-100 text-lg mb-8 max-w-2xl mx-auto">
              Join thousands of businesses that trust FetanVerify for their verification needs.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <button className="bg-white text-primary-600 hover:bg-gray-50 px-8 py-3 rounded-lg font-medium transition-colors duration-200">
                Start Free Trial
              </button>
              <button className="border-2 border-white text-white hover:bg-white hover:text-primary-600 px-8 py-3 rounded-lg font-medium transition-colors duration-200">
                Contact Sales
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}