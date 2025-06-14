import React from 'react';
import { Shield, CheckCircle, Clock, Users } from 'lucide-react';

export function Hero() {
  return (
    <section className="relative bg-gradient-to-br from-primary-50 via-white to-primary-50 py-20 lg:py-28">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          {/* Content */}
          <div className="animate-fade-in">
            <div className="inline-flex items-center px-4 py-2 bg-primary-100 text-primary-700 rounded-full text-sm font-medium mb-6">
              <Shield className="w-4 h-4 mr-2" />
              Trusted by 10,000+ businesses
            </div>
            
            <h1 className="text-4xl lg:text-6xl font-bold text-gray-900 mb-6 leading-tight">
              Secure Verification
              <span className="text-primary-600 block">Made Simple</span>
            </h1>
            
            <p className="text-xl text-gray-600 mb-8 leading-relaxed">
              Streamline your verification processes with our advanced, secure platform. 
              Fast, reliable, and compliant with global standards.
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 mb-12">
              <button className="btn-primary px-8 py-3 text-base">
                Start Verification
              </button>
              <button className="btn-secondary px-8 py-3 text-base">
                Watch Demo
              </button>
            </div>
            
            {/* Trust indicators */}
            <div className="grid grid-cols-3 gap-6 pt-8 border-t border-gray-200">
              <div className="text-center">
                <CheckCircle className="w-8 h-8 text-success-500 mx-auto mb-2" />
                <p className="text-sm font-medium text-gray-900">99.9% Accuracy</p>
              </div>
              <div className="text-center">
                <Clock className="w-8 h-8 text-primary-500 mx-auto mb-2" />
                <p className="text-sm font-medium text-gray-900">< 30 Seconds</p>
              </div>
              <div className="text-center">
                <Users className="w-8 h-8 text-warning-500 mx-auto mb-2" />
                <p className="text-sm font-medium text-gray-900">24/7 Support</p>
              </div>
            </div>
          </div>
          
          {/* Visual */}
          <div className="relative animate-fade-in">
            <div className="relative">
              {/* Main card */}
              <div className="card p-8 transform rotate-3 hover:rotate-0 transition-transform duration-300">
                <div className="flex items-center mb-6">
                  <div className="w-12 h-12 bg-primary-100 rounded-full flex items-center justify-center">
                    <Shield className="w-6 h-6 text-primary-600" />
                  </div>
                  <div className="ml-4">
                    <h3 className="font-semibold text-gray-900">Verification Status</h3>
                    <p className="text-sm text-gray-600">Document Processing</p>
                  </div>
                </div>
                
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Identity Check</span>
                    <div className="flex items-center text-success-600">
                      <CheckCircle className="w-4 h-4 mr-1" />
                      <span className="text-sm font-medium">Verified</span>
                    </div>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Document Scan</span>
                    <div className="flex items-center text-success-600">
                      <CheckCircle className="w-4 h-4 mr-1" />
                      <span className="text-sm font-medium">Complete</span>
                    </div>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Background Check</span>
                    <div className="flex items-center text-warning-600">
                      <Clock className="w-4 h-4 mr-1" />
                      <span className="text-sm font-medium">Processing</span>
                    </div>
                  </div>
                </div>
                
                <div className="mt-6 pt-6 border-t border-gray-200">
                  <div className="flex items-center justify-between">
                    <span className="font-medium text-gray-900">Overall Status</span>
                    <span className="px-3 py-1 bg-success-100 text-success-700 rounded-full text-sm font-medium">
                      85% Complete
                    </span>
                  </div>
                </div>
              </div>
              
              {/* Floating elements */}
              <div className="absolute -top-4 -right-4 w-16 h-16 bg-primary-500 rounded-full flex items-center justify-center shadow-lg animate-pulse-slow">
                <Shield className="w-8 h-8 text-white" />
              </div>
              
              <div className="absolute -bottom-4 -left-4 w-12 h-12 bg-success-500 rounded-full flex items-center justify-center shadow-lg">
                <CheckCircle className="w-6 h-6 text-white" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
  );
}