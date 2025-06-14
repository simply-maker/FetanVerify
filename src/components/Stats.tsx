import React from 'react';

export function Stats() {
  const stats = [
    {
      number: '10,000+',
      label: 'Businesses Trust Us',
      description: 'Companies worldwide rely on our platform',
    },
    {
      number: '1M+',
      label: 'Verifications Completed',
      description: 'Documents processed with 99.9% accuracy',
    },
    {
      number: '190+',
      label: 'Countries Supported',
      description: 'Global coverage with local compliance',
    },
    {
      number: '<30s',
      label: 'Average Processing Time',
      description: 'Lightning-fast verification results',
    },
  ];

  return (
    <section className="py-16 bg-white border-t border-gray-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-8">
          {stats.map((stat, index) => (
            <div key={index} className="text-center">
              <div className="text-3xl lg:text-4xl font-bold text-primary-600 mb-2">
                {stat.number}
              </div>
              <div className="text-lg font-semibold text-gray-900 mb-1">
                {stat.label}
              </div>
              <div className="text-sm text-gray-600">
                {stat.description}
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}