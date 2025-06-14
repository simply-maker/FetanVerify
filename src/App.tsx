import React, { useState } from 'react';
import { Header } from './components/Header';
import { Hero } from './components/Hero';
import { VerificationForm } from './components/VerificationForm';
import { Features } from './components/Features';
import { Stats } from './components/Stats';
import { Footer } from './components/Footer';

function App() {
  const [activeSection, setActiveSection] = useState('home');

  return (
    <div className="min-h-screen bg-gray-50">
      <Header activeSection={activeSection} setActiveSection={setActiveSection} />
      
      <main>
        {activeSection === 'home' && (
          <>
            <Hero />
            <Stats />
            <Features />
          </>
        )}
        
        {activeSection === 'verify' && <VerificationForm />}
        
        {activeSection === 'about' && (
          <div className="py-20">
            <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
              <div className="text-center mb-12">
                <h1 className="text-4xl font-bold text-gray-900 mb-4">About FetanVerify</h1>
                <p className="text-xl text-gray-600 max-w-3xl mx-auto">
                  We're dedicated to providing secure, reliable, and efficient verification services 
                  for businesses and individuals worldwide.
                </p>
              </div>
              
              <div className="grid md:grid-cols-2 gap-12 items-center">
                <div>
                  <h2 className="text-2xl font-semibold text-gray-900 mb-4">Our Mission</h2>
                  <p className="text-gray-600 mb-6">
                    To make verification processes seamless, secure, and accessible to everyone. 
                    We believe in building trust through technology and transparency.
                  </p>
                  
                  <h2 className="text-2xl font-semibold text-gray-900 mb-4">Why Choose Us?</h2>
                  <ul className="space-y-3 text-gray-600">
                    <li className="flex items-center">
                      <div className="w-2 h-2 bg-primary-500 rounded-full mr-3"></div>
                      Industry-leading security standards
                    </li>
                    <li className="flex items-center">
                      <div className="w-2 h-2 bg-primary-500 rounded-full mr-3"></div>
                      24/7 customer support
                    </li>
                    <li className="flex items-center">
                      <div className="w-2 h-2 bg-primary-500 rounded-full mr-3"></div>
                      Fast and reliable processing
                    </li>
                    <li className="flex items-center">
                      <div className="w-2 h-2 bg-primary-500 rounded-full mr-3"></div>
                      Compliance with global regulations
                    </li>
                  </ul>
                </div>
                
                <div className="card p-8">
                  <h3 className="text-xl font-semibold text-gray-900 mb-4">Get in Touch</h3>
                  <div className="space-y-4">
                    <div>
                      <p className="text-sm font-medium text-gray-700">Email</p>
                      <p className="text-gray-600">support@fetanverify.com</p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-700">Phone</p>
                      <p className="text-gray-600">+1 (555) 123-4567</p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-700">Address</p>
                      <p className="text-gray-600">123 Verification St, Tech City, TC 12345</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
      
      <Footer />
    </div>
  );
}

export default App;