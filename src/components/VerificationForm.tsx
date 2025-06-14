import React, { useState } from 'react';
import { Upload, FileText, User, Mail, Phone, Shield, CheckCircle, AlertCircle } from 'lucide-react';

export function VerificationForm() {
  const [step, setStep] = useState(1);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    documentType: '',
    file: null as File | null,
  });
  const [isProcessing, setIsProcessing] = useState(false);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFormData({
        ...formData,
        file: e.target.files[0],
      });
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsProcessing(true);
    
    // Simulate processing
    setTimeout(() => {
      setIsProcessing(false);
      setStep(4);
    }, 3000);
  };

  const nextStep = () => {
    if (step < 3) setStep(step + 1);
  };

  const prevStep = () => {
    if (step > 1) setStep(step - 1);
  };

  return (
    <section className="py-20 bg-gray-50">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold text-gray-900 mb-4">Document Verification</h1>
          <p className="text-xl text-gray-600">
            Secure and fast verification process in just a few steps
          </p>
        </div>

        {/* Progress Bar */}
        <div className="mb-12">
          <div className="flex items-center justify-center">
            {[1, 2, 3, 4].map((stepNumber) => (
              <React.Fragment key={stepNumber}>
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-medium transition-colors duration-200 ${
                    step >= stepNumber
                      ? 'bg-primary-600 text-white'
                      : 'bg-gray-200 text-gray-600'
                  }`}
                >
                  {step > stepNumber ? (
                    <CheckCircle className="w-5 h-5" />
                  ) : (
                    stepNumber
                  )}
                </div>
                {stepNumber < 4 && (
                  <div
                    className={`w-16 h-1 mx-2 transition-colors duration-200 ${
                      step > stepNumber ? 'bg-primary-600' : 'bg-gray-200'
                    }`}
                  />
                )}
              </React.Fragment>
            ))}
          </div>
          <div className="flex justify-center mt-4">
            <div className="grid grid-cols-4 gap-8 text-center">
              <span className={`text-sm ${step >= 1 ? 'text-primary-600 font-medium' : 'text-gray-500'}`}>
                Personal Info
              </span>
              <span className={`text-sm ${step >= 2 ? 'text-primary-600 font-medium' : 'text-gray-500'}`}>
                Document Type
              </span>
              <span className={`text-sm ${step >= 3 ? 'text-primary-600 font-medium' : 'text-gray-500'}`}>
                Upload & Review
              </span>
              <span className={`text-sm ${step >= 4 ? 'text-primary-600 font-medium' : 'text-gray-500'}`}>
                Complete
              </span>
            </div>
          </div>
        </div>

        <div className="card p-8 max-w-2xl mx-auto">
          {step === 1 && (
            <div className="animate-fade-in">
              <div className="flex items-center mb-6">
                <User className="w-6 h-6 text-primary-600 mr-3" />
                <h2 className="text-2xl font-semibold text-gray-900">Personal Information</h2>
              </div>
              
              <div className="grid md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    First Name
                  </label>
                  <input
                    type="text"
                    name="firstName"
                    value={formData.firstName}
                    onChange={handleInputChange}
                    className="input"
                    placeholder="Enter your first name"
                    required
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Last Name
                  </label>
                  <input
                    type="text"
                    name="lastName"
                    value={formData.lastName}
                    onChange={handleInputChange}
                    className="input"
                    placeholder="Enter your last name"
                    required
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Email Address
                  </label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                      type="email"
                      name="email"
                      value={formData.email}
                      onChange={handleInputChange}
                      className="input pl-10"
                      placeholder="Enter your email"
                      required
                    />
                  </div>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Phone Number
                  </label>
                  <div className="relative">
                    <Phone className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                      type="tel"
                      name="phone"
                      value={formData.phone}
                      onChange={handleInputChange}
                      className="input pl-10"
                      placeholder="Enter your phone number"
                      required
                    />
                  </div>
                </div>
              </div>
              
              <div className="flex justify-end mt-8">
                <button
                  onClick={nextStep}
                  disabled={!formData.firstName || !formData.lastName || !formData.email || !formData.phone}
                  className="btn-primary px-6 py-2"
                >
                  Continue
                </button>
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="animate-fade-in">
              <div className="flex items-center mb-6">
                <FileText className="w-6 h-6 text-primary-600 mr-3" />
                <h2 className="text-2xl font-semibold text-gray-900">Document Type</h2>
              </div>
              
              <p className="text-gray-600 mb-6">
                Select the type of document you'd like to verify
              </p>
              
              <div className="grid md:grid-cols-2 gap-4">
                {[
                  { id: 'passport', name: 'Passport', icon: '🛂' },
                  { id: 'drivers-license', name: "Driver's License", icon: '🚗' },
                  { id: 'national-id', name: 'National ID', icon: '🆔' },
                  { id: 'other', name: 'Other Document', icon: '📄' },
                ].map((doc) => (
                  <label
                    key={doc.id}
                    className={`cursor-pointer p-4 border-2 rounded-lg transition-all duration-200 hover:border-primary-300 ${
                      formData.documentType === doc.id
                        ? 'border-primary-500 bg-primary-50'
                        : 'border-gray-200'
                    }`}
                  >
                    <input
                      type="radio"
                      name="documentType"
                      value={doc.id}
                      onChange={handleInputChange}
                      className="sr-only"
                    />
                    <div className="flex items-center">
                      <span className="text-2xl mr-3">{doc.icon}</span>
                      <span className="font-medium text-gray-900">{doc.name}</span>
                    </div>
                  </label>
                ))}
              </div>
              
              <div className="flex justify-between mt-8">
                <button onClick={prevStep} className="btn-secondary px-6 py-2">
                  Back
                </button>
                <button
                  onClick={nextStep}
                  disabled={!formData.documentType}
                  className="btn-primary px-6 py-2"
                >
                  Continue
                </button>
              </div>
            </div>
          )}

          {step === 3 && (
            <div className="animate-fade-in">
              <div className="flex items-center mb-6">
                <Upload className="w-6 h-6 text-primary-600 mr-3" />
                <h2 className="text-2xl font-semibold text-gray-900">Upload Document</h2>
              </div>
              
              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Upload your document
                </label>
                <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-primary-400 transition-colors duration-200">
                  <Upload className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-600 mb-2">
                    Drag and drop your file here, or{' '}
                    <label className="text-primary-600 cursor-pointer hover:text-primary-700">
                      browse
                      <input
                        type="file"
                        onChange={handleFileChange}
                        accept=".jpg,.jpeg,.png,.pdf"
                        className="sr-only"
                      />
                    </label>
                  </p>
                  <p className="text-sm text-gray-500">
                    Supported formats: JPG, PNG, PDF (Max 10MB)
                  </p>
                  
                  {formData.file && (
                    <div className="mt-4 p-3 bg-primary-50 rounded-lg">
                      <p className="text-sm font-medium text-primary-700">
                        {formData.file.name}
                      </p>
                    </div>
                  )}
                </div>
              </div>
              
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
                <div className="flex">
                  <AlertCircle className="w-5 h-5 text-yellow-600 mr-2 flex-shrink-0 mt-0.5" />
                  <div>
                    <h3 className="text-sm font-medium text-yellow-800">Important Guidelines</h3>
                    <ul className="mt-2 text-sm text-yellow-700 list-disc list-inside space-y-1">
                      <li>Ensure the document is clearly visible and not blurry</li>
                      <li>All corners of the document should be visible</li>
                      <li>Avoid shadows or glare on the document</li>
                    </ul>
                  </div>
                </div>
              </div>
              
              <div className="flex justify-between">
                <button onClick={prevStep} className="btn-secondary px-6 py-2">
                  Back
                </button>
                <button
                  onClick={handleSubmit}
                  disabled={!formData.file || isProcessing}
                  className="btn-primary px-6 py-2"
                >
                  {isProcessing ? 'Processing...' : 'Submit for Verification'}
                </button>
              </div>
            </div>
          )}

          {step === 4 && (
            <div className="animate-fade-in text-center">
              <div className="w-16 h-16 bg-success-100 rounded-full flex items-center justify-center mx-auto mb-6">
                <CheckCircle className="w-8 h-8 text-success-600" />
              </div>
              
              <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                Verification Complete!
              </h2>
              
              <p className="text-gray-600 mb-8">
                Your document has been successfully verified. You'll receive a confirmation email shortly.
              </p>
              
              <div className="bg-gray-50 rounded-lg p-6 mb-8">
                <h3 className="font-medium text-gray-900 mb-4">Verification Summary</h3>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-600">Name:</span>
                    <span className="font-medium">{formData.firstName} {formData.lastName}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Document Type:</span>
                    <span className="font-medium capitalize">{formData.documentType?.replace('-', ' ')}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Status:</span>
                    <span className="text-success-600 font-medium">Verified</span>
                  </div>
                </div>
              </div>
              
              <button
                onClick={() => {
                  setStep(1);
                  setFormData({
                    firstName: '',
                    lastName: '',
                    email: '',
                    phone: '',
                    documentType: '',
                    file: null,
                  });
                }}
                className="btn-primary px-6 py-2"
              >
                Start New Verification
              </button>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}